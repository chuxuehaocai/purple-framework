package dev.naominet.purple.framework.utils

import java.io.File
import kotlin.io.encoding.Base64

class MessageBuilder {
    private val sb = StringBuilder()

    fun append(msg: String): MessageBuilder {
        sb.append(msg)
        return this
    }

    fun at(targetId: Long): MessageBuilder {
        sb.append("[CQ:at,qq=$targetId]")
        return this
    }

    fun image(file: File): MessageBuilder {
        val code = String.format("[CQ:image,file=%s]", escape(file.canonicalPath))
        sb.append(code)
        return this
    }

    private fun escape(string: String): String {
        return string
            .replace("&", "&amp;")
            .replace(",", "&#44;")
            .replace("[", "&#91;")
            .replace("]", "&#93;")
    }

    fun reply(targetMsgId: Long): MessageBuilder {
        sb.append("[CQ:reply,id=$targetMsgId]")
        return this
    }

    fun replyGroup(targetMsgId: Long, message: String): MessageBuilder {
        sb.append("[CQ:reply,id=${targetMsgId}]$message")
        return this
    }

    fun build(): String {
        val temp = sb.toString()
        sb.clear()
        return temp
    }

    override fun toString(): String = sb.toString()
}