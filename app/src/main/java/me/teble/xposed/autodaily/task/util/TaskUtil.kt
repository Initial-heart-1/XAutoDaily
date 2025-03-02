package me.teble.xposed.autodaily.task.util

import cn.hutool.core.util.ReUtil
import cn.hutool.cron.pattern.CronPattern
import cn.hutool.cron.pattern.CronPatternUtil
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import function.task.module.MsgExtract
import function.task.module.Task
import me.teble.xposed.autodaily.hook.config.Config.accountConfig
import me.teble.xposed.autodaily.hook.utils.ToastUtil
import me.teble.xposed.autodaily.task.request.ReqFactory
import me.teble.xposed.autodaily.task.request.enum.ReqType
import me.teble.xposed.autodaily.task.request.model.TaskResponse
import me.teble.xposed.autodaily.task.util.Const.ENV_VARIABLE
import me.teble.xposed.autodaily.task.util.Const.LAST_EXEC_TIME
import me.teble.xposed.autodaily.task.util.Const.NEXT_SHOULD_EXEC_TIME
import me.teble.xposed.autodaily.task.util.EnvFormatUtil.format
import me.teble.xposed.autodaily.task.util.EnvFormatUtil.formatList
import me.teble.xposed.autodaily.utils.LogUtil
import me.teble.xposed.autodaily.utils.toJsonString
import java.util.*

object TaskUtil {
    private const val TAG = "TaskUtil"
    private val jsonPathConf = Configuration.defaultConfiguration()
        .addOptions(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)

    fun execute(
        reqType: ReqType,
        task: Task,
        relayMap: Map<String, Task>,
        env: MutableMap<String, Any>
    ): Boolean {
        LogUtil.d(TAG, "开始执行任务：${task.id}")
        val relays: List<Task> = task.relay?.let {
            mutableListOf<Task>().apply {
                it.split("|").forEach {
                    add(relayMap[it] ?: throw RuntimeException("未知的依赖任务：${it}"))
                }
            }
        } ?: emptyList()
        // 将用户自定义变量的值放进环境变量
        task.envs?.forEach {
            val confValue = getConfEnv(task.id, it.name)
            // 变量默认值为空表示强制要求用户填写，否则抛出异常
            if (it.default.isEmpty()) {
                if (confValue == null || confValue.isEmpty()) {
                    ToastUtil.send("任务【${task.id}】的变量${it.name}不能为空", true)
                    throw RuntimeException("任务【${task.id}】的变量${it.name}不能为空")
                }
            }
            when (it.type) {
                "string" -> {
                    env[it.name] = confValue ?: it.default
                }
                "list", "friend", "group" -> {
                    val value = confValue ?: it.default
                    env[it.name] = value.split(",").toMutableList()
                }
                else -> throw RuntimeException("未处理的变量类型：${it.type}")
            }
            LogUtil.d(TAG, "开始获取用户自定义变量：${it.name}, 用户自定义值为：${confValue}, 默认值：${it.default}")
        }
        // 执行依赖任务
        relays.forEach {
            LogUtil.d(TAG, "开始执行依赖任务列表: ${task.relay}")
            // 依赖任务失败，退出执行
            if (!execute(reqType, it, relayMap, env)) {
                // TODO 多次失败，任务禁用
                return false
            }
        }
        val taskReqUtil = ReqFactory.getReq(reqType)
        val requests = taskReqUtil.create(task, env)
        var successNum = 0
        lateinit var lastErrMsg: String
        requests.forEachIndexed { index, it ->
            Thread.sleep((task.delay * 1000).toLong())
            val response = taskReqUtil.executor(it)
            val result = handleCallback(response, task, env)
            if (result.success) {
                successNum++
            } else {
                lastErrMsg = result.errMsg
                LogUtil.i(TAG, "任务【${task.id}】执行失败: $lastErrMsg")
            }
        }
        // 正常cron任务，需要计算下次执行时间
        if (task.cron != null && task.cron != "basic") {
            val currentTime = Date()
            accountConfig.putString("${task.id}#${LAST_EXEC_TIME}", currentTime.format())
            val nextTime =
                CronPatternUtil.nextDateAfter(CronPattern(task.cron), currentTime, true)
            accountConfig.putString("${task.id}#${NEXT_SHOULD_EXEC_TIME}", nextTime.format())
            ConfigUtil.saveAndCheckMostRecentExecTime(nextTime)
            accountConfig.putString(
                "${task.id}#${Const.LAST_EXEC_MSG}",
                if (requests.size == 1) {
                    if (successNum == 1) {
                        ToastUtil.send("任务【${task.id}】执行成功")
                        "执行成功"
                    } else {
                        ToastUtil.send("任务【${task.id}】执行失败：$lastErrMsg")
                        lastErrMsg
                    }
                } else {
                    LogUtil.i(TAG, "任务【${task.id}】执行完毕，成功${successNum}个，失败${requests.size - successNum}个")
                    ToastUtil.send("任务【${task.id}】执行完毕，成功${successNum}个，失败${requests.size - successNum}个")
                    "执行成功${successNum}个，失败${requests.size - successNum}个"
                }
            )
        }
        return successNum > 0
    }

    private fun handleCallback(
        response: TaskResponse,
        task: Task,
        env: MutableMap<String, Any>
    ): CallbackResult {
        val callback = task.callback
        var data: String? = response.body.trim()
        callback.dataRegex?.let {
            data = ReUtil.getGroup1(callback.dataRegex, data)
            LogUtil.d(TAG, "handleCallback -> 正则处理后data为: $data")
        }
        // 是否合理
        data ?: throw RuntimeException("响应为空")
        extract(data!!, response.headersText, callback.extracts, env)
        val success: Boolean = if (callback.assert == null) {
            response.code == 200
        } else {
            format(callback.assert.key, env) == format(callback.assert.value, env)
        }
        var errMsg = "执行失败"
        callback.errMsg?.let {
            val msg = format(it, env)
            if (msg.isNotEmpty()) errMsg = msg
        }
        callback.replaces?.forEach {
            errMsg = ReUtil.replaceAll(errMsg, it.match, it.replacement)
        }
        LogUtil.d(
            TAG, """handleCallback -> 
            |   success: $success
            |   errMsg: $errMsg
        """.trimMargin()
        )
        return CallbackResult(success = success, errMsg = errMsg)
    }

    private fun extract(
        data: String,
        headersText: String?,
        extracts: List<MsgExtract>?,
        env: MutableMap<String, Any>
    ) {
        var documentContext: DocumentContext? = null
        if (data.startsWith("{") && data.endsWith("}")) {
            documentContext = JsonPath.using(jsonPathConf).parse(data)
        }
        extracts?.forEach {
            LogUtil.d(TAG, "开始提取变量 -> ${it.toJsonString()}")
            val res: Any
            if (it.match.startsWith("$")) {
                try {
                    res = documentContext!!.read(it.match) ?: ""
                    LogUtil.d(
                        TAG,
                        "JsonPath从 body 提取变量: ${it.match}, ${it.key} = ${res.toJsonString()}"
                    )
                } catch (e: Exception) {
                    LogUtil.e(TAG, e)
                    throw e
                }
            } else {
                res = ReUtil.getGroup1(it.match, if (it.from == "headers") headersText else data)
                    ?: ""
                LogUtil.d(TAG, "正则从 ${it.from} 提取变量: ${it.match}, ${it.key} = $res")
            }
            env["this"] = res
            if (res is List<*>) {
                if (!env.containsKey(it.key)) {
                    env[it.key] = formatList(it.value, env)
                } else {
                    env[it.key] = (env[it.key] as List<*>).plus(formatList(it.value, env))
                }
            } else {
                env[it.key] = format(it.value, env)
            }
            env.remove("this")
        }
    }

    private fun getConfEnv(taskId: String, key: String): String? {
        return accountConfig.getString("$taskId#$ENV_VARIABLE#$key")
    }

    data class CallbackResult(
        val success: Boolean,
        val errMsg: String
    )
}