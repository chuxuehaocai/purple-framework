package dev.naominet.purple.framework.beans

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TextMessageBean(
    val self_id: Long = 0,
    val user_id: Long = 0,
    val time: Long = 0,
    val message_id: Long = 0,
    val message_seq: Long = 0,
    val real_id: Long = 0,
    val real_seq: String = "",
    val message_type: String = "",
    val sender: SenderBean = SenderBean(),
    val raw_message: String = "",
    val font: Int = 0,
    val sub_type: String = "",
    val message: List<JsonElement> = emptyList(),
    val message_format: String = "",
    val post_type: String = "",
    val group_id: Long = 0,
    val group_name: String = "",
)

@Serializable
data class SenderBean(
    val user_id: Long = 0,
    val nickname: String = "",
    val card: String = "",
    val role: String = "",
)

@Serializable
data class TextMessageSegmentBean(
    val type: String = "text",
    val data: TextMessageDataBean = TextMessageDataBean(),
)

@Serializable
data class TextMessageDataBean(
    val text: String = "",
)
