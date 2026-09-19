package dev.naominet.purple.framework.core

import dev.naominet.purple.framework.utils.MessageBuilder
import java.nio.charset.Charset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

object Bot {
    fun sendGroupMessage(groupId: Long, msg: String, autoEscaped: Boolean = false) {
        val json = Json.encodeToString(buildJsonObject {
            put("action", "send_group_msg")
            putJsonObject("params") {
                put("group_id", groupId)
                put("message", msg)
                put("auto_escape", autoEscaped)
            }
        })

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }

    fun sendPrivateMessage(userId: Long, msg: String, autoEscaped: Boolean = false) {
        val json = Json.encodeToString(buildJsonObject {
            put("action", "send_private_msg")
            putJsonObject("params") {
                put("user_id", userId)
                put("message", msg)
                put("auto_escape", autoEscaped)
            }
        })

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }

    fun deleteMessage(msgId: Long) {
        val json = Json.encodeToString(buildJsonObject {
            put("action", "delete_msg")
            putJsonObject("params") { put("message_id", msgId) }
        })

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }

    fun sendLike(userId: Long, count: Int) {
        val json = Json.encodeToString(buildJsonObject {
            put("action", "send_like")
            putJsonObject("params") {
                put("user_id", userId)
                put("times", count)
            }
        })

        PurpleFramework.transport.send(json.toByteArray(Charset.forName(PurpleFramework.configuration.encoding)))
    }
}
