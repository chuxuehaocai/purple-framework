package dev.naominet.purple.framework.event

import dev.naominet.purple.framework.beans.TextMessageBean

object EventManager {
    val groupMessageEvent = Event<TextMessageBean>()
    val privateMessageEvent = Event<TextMessageBean>()
}