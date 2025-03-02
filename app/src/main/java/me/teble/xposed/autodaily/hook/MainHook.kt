package me.teble.xposed.autodaily.hook

import android.app.AndroidAppHelper
import android.content.res.XModuleResources
import cn.hutool.core.util.ReflectUtil.*
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.emptyParam
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.teble.xposed.autodaily.BuildConfig
import me.teble.xposed.autodaily.config.QQClasses.Companion.LoadData
import me.teble.xposed.autodaily.dex.utils.DexKit.findSubClasses
import me.teble.xposed.autodaily.dex.utils.DexKit.locateClasses
import me.teble.xposed.autodaily.hook.base.BaseHook
import me.teble.xposed.autodaily.hook.base.Global
import me.teble.xposed.autodaily.hook.base.Global.init
import me.teble.xposed.autodaily.hook.base.Global.initContext
import me.teble.xposed.autodaily.hook.base.Global.qqVersionCode
import me.teble.xposed.autodaily.hook.base.Initiator
import me.teble.xposed.autodaily.hook.base.Initiator.loadAs
import me.teble.xposed.autodaily.hook.base.XAClassLoader
import me.teble.xposed.autodaily.hook.config.Config
import me.teble.xposed.autodaily.hook.config.Config.confuseInfo
import me.teble.xposed.autodaily.hook.config.Config.hooksVersion
import me.teble.xposed.autodaily.hook.enums.QQTypeEnum
import me.teble.xposed.autodaily.hook.proxy.ProxyManager
import me.teble.xposed.autodaily.hook.proxy.activity.ResInjectUtil
import me.teble.xposed.autodaily.hook.utils.ToastUtil
import me.teble.xposed.autodaily.utils.LogUtil

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        private const val TAG = "MainHook"
    }

    private lateinit var subHookClasses: Set<String>
    private lateinit var loadPackageParam: LoadPackageParam

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        Global.modulePath = startupParam.modulePath
        Global.moduleRes = XModuleResources.createInstance(Global.modulePath, null)
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        LogUtil.i(TAG, "handleLoadPackage: $loadPackageParam")
        this.loadPackageParam = loadPackageParam
        if (!QQTypeEnum.contain(loadPackageParam.packageName) || !loadPackageParam.isFirstApplication) return
        init
    }

    val init by lazy {
        LogUtil.d(TAG, "current process: ${loadPackageParam.processName}")
        EzXHelperInit.initHandleLoadPackage(loadPackageParam)
        EzXHelperInit.setLogTag("XAutoDaily")
        EzXHelperInit.setToastTag("XAutoDaily")
        doInit()
    }

    private fun doInit() {
        // 初始化全局ClassLoader
        Initiator.init(loadPackageParam.classLoader)
        init(loadPackageParam)
        // 替换classloader
        replaceParentClassloader(loadPackageParam.classLoader)
        LogUtil.d(TAG, "loadPackageParam.packageName -> ${loadPackageParam.packageName}")
        // TODO 分进程处理
        if (loadPackageParam.processName != loadPackageParam.packageName) {
            return
        }
        CoreServiceHook().coreServiceHook()

        var unhook: XC_MethodHook.Unhook? = null

        unhook =
            findMethod(LoadData) { returnType == Boolean::class.java && emptyParam }.hookAfter {
                // 防止hook多次被执行
                unhook?.unhook()
                val context = AndroidAppHelper.currentApplication()
                // 初始化全局Context
                initContext(context)
                EzXHelperInit.initAppContext(context)
                // MMKV
                Config.init()
                //加载资源注入
                ResInjectUtil.injectRes(context.resources)
                //初始化代理
                ProxyManager.init()
                // dex相关
                if (Global.hostProcessName == "") {
                    doDexInit()
                }
                //初始化hook
                initHook()
            }
    }

    private fun doDexInit() {
        // dex解析
        // module dex
        val cache = Config.classCache
        if (cache.getInt("moduleVersion", 0) < BuildConfig.VERSION_CODE || BuildConfig.DEBUG) {
            LogUtil.d(TAG, "模块版本更新/Debug版本，重新定位Hook子类")
            subHookClasses =
                findSubClasses(MainHook::class.java.classLoader!!, BaseHook::class.java).toSet()
            cache.putStringSet("subHookClasses", subHookClasses)
            cache.putInt("moduleVersion", BuildConfig.VERSION_CODE)
        } else {
            LogUtil.d(TAG, "缓存生效，跳过hook子类解析")
            subHookClasses = cache.getStringSet("subHookClasses") ?: emptySet()
        }
        // qq dex
        val confuseInfoKeys = confuseInfo.keys
        val needLocateClasses = confuseInfoKeys.toHashSet()
        // 清空混淆缓存
        if (cache.getInt("hooksVersion", 0) < hooksVersion) {
            LogUtil.d(TAG, "清空Hooks缓存")
            cache.clearAll()
            cache.putInt("hooksVersion", hooksVersion)
        }
        // 加入修改了特征的类
        confuseInfo.forEach {
            val key = "${it.key}#hash"
            val hash = it.value.hashCode()
            if (cache.getInt(key, 0) != hash) {
                cache.putInt(key, hash)
                needLocateClasses.add(it.key)
            }
        }
        // 尝试获取，成功则加入新版缓存
        needLocateClasses.removeIf { classSimpleName ->
            LogUtil.d(TAG, "尝试获取类：$classSimpleName")
            Global.hostClassLoader.loadClass(classSimpleName)?.let {
                LogUtil.d(TAG, "尝试获取类成功 -> ${it.canonicalName}")
                cache.putString("$classSimpleName#${qqVersionCode}", classSimpleName)
                true
            } ?: false
        }
        if (needLocateClasses.isEmpty()) {
//            ToastUtil.send("无需重新定位，跳过执行")
            return
        }
        LogUtil.log("needLocateClasses -> $needLocateClasses")
        ToastUtil.send("正在尝试定位QQ混淆类")
        val info = needLocateClasses.associateWith { confuseInfo[it] }
        val startTime = System.currentTimeMillis()
        val locateRes = locateClasses(info)
        val useTime = System.currentTimeMillis() - startTime
        var locateNum = 0
        locateRes.forEach {
            if (it.value.size == 1) {
                LogUtil.i(TAG, "locate info: ${it.key} -> ${it.value[0]}")
                cache.putString("${it.key}#${qqVersionCode}", it.value[0])
                locateNum++
            } else {
                LogUtil.w(TAG, "locate not instance class: ${it.key} -> ${it.value}")
                cache.putString("${it.key}#${qqVersionCode}", null)
            }
        }
        cache.putStringSet("confuseClasses", confuseInfoKeys)
        ToastUtil.send("dex搜索完毕，成功${locateNum}个，失败${needLocateClasses.size - locateNum}个，耗时${useTime}ms")
    }

    private fun replaceParentClassloader(qClassloader: ClassLoader) {
        val fParent = getField(ClassLoader::class.java, "parent")
        val mClassloader = MainHook::class.java.classLoader
        val parentClassloader = getFieldValue(mClassloader, "parent") as ClassLoader
        try {
            if (XAClassLoader::class.java != parentClassloader.javaClass) {
                LogUtil.d(TAG, "replace parent classloader")
                setFieldValue(mClassloader, fParent, XAClassLoader(qClassloader, parentClassloader))
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    private fun initHook() {
        LogUtil.d(TAG, "initHook: ->$this")
        for (cls in subHookClasses) {
            try {
                LogUtil.d(TAG, "hook class -> $cls")
                getConstructor(loadAs<BaseHook>(cls, Global.moduleClassLoader)).newInstance().init()
            } catch (e: Exception) {
                LogUtil.e(TAG, e)
            }
        }
        LogUtil.i("模块加载完毕")
    }
}