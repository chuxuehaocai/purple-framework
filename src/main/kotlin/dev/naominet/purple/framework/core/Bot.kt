package dev.naominet.purple.framework.core

import com.alibaba.fastjson2.JSON
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
}
