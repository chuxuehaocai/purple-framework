package dev.naominet.purple.framework.utils

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

    fun reply(targetMsgId: Long): MessageBuilder {
        sb.append("[CQ:reply,id=$targetMsgId]")
        return this
    }

    fun replyGroup(targetMsgId: Long, message: String): MessageBuilder {
        sb.append("[CQ:reply,id=${targetMsgId}]$message")
        return this
    }

    fun build(): String = sb.toString()

    override fun toString(): String = sb.toString()
}