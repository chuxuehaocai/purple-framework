package dev.naominet.purple.framework.transport

import dev.naominet.purple.framework.event.Event

interface ITransport {
    val onReceiveData: Event<ByteArray>
    fun connect()
    fun send(data: ByteArray)
}
