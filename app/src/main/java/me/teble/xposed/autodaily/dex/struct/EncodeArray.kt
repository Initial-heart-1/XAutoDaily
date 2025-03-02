package me.teble.xposed.autodaily.dex.struct

import me.teble.xposed.autodaily.dex.utils.ByteUtils
import java.util.*

class EncodeArray {
    var size = 0
    lateinit var values: Array<EncodeValue>
    fun findStrIdx(strIdx: Int): Boolean {
        for (encodeValue in values) {
            if (encodeValue.findStrIdx(strIdx)) {
                return true
            }
        }
        return false
    }

    val strIdSet: Set<Int>
        get() {
            val set: MutableSet<Int> = HashSet()
            for (encodeValue in values) {
                set.addAll(encodeValue.strIdSet)
            }
            return set
        }

    companion object {
        fun parser(src: ByteArray?, index: IntArray): EncodeArray {
            val encodeArray = EncodeArray()
            encodeArray.size = ByteUtils.readUleb128(src, index)
            encodeArray.values = Array(encodeArray.size) { EncodeValue.parser(src, index) }
//            for (i in 0 until encodeArray.size) {
//                encodeArray.values[i] = EncodeValue.parser(src, index)
//            }
            return encodeArray
        }
    }
}