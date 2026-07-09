package dev.naominet.purple.framework.core

import com.alibaba.fastjson2.JSON
import dev.naominet.purple.framework.utils.MessageBuilder
import java.nio.charset.Charset

object Bot {
    fun sendGroupMessage(groupId: Long, msg: String, autoEscaped: Boolean = false) {
        val json = JSON.toJSONString(
            mapOf(
                "action" to "send_group_msg",
                "params" to mapOf(
                    "group_id" to groupId,
                    "message" to msg,
                    "auto_escape" to autoEscaped,
                ),
            )
        )

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }

    fun sendPrivateMessage(userId: Long, msg: String, autoEscaped: Boolean = false) {
        val json = JSON.toJSONString(
            mapOf(
                "action" to "send_private_msg",
                "params" to mapOf(
                    "user_id" to userId,
                    "message" to msg,
                    "auto_escape" to autoEscaped,
                ),
            )
        )

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }

    fun deleteMessage(msgId: Long) {
        val json = JSON.toJSONString(
            mapOf(
                "action" to "delete_msg",
                "params" to mapOf(
                    "message_id" to msgId
                ),
            )
        )

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }

    fun sendLike(userId: Long, count: Int) {
        val json = JSON.toJSONString(
            mapOf(
                "action" to "send_like",
                "params" to mapOf(
                    "user_id" to userId,
                    "times" to count
                ),
            )
        )

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }
}
