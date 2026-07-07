package dev.naominet.purple.framework.transport.impl

import dev.naominet.purple.framework.event.Event
import dev.naominet.purple.framework.transport.ITransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WebsocketClient(val host: String, val port: Int, val path: String) : ITransport {
    private val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.websocket.WebSockets)
    }
    override val onReceiveData = Event<ByteArray>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var receiveJob: Job? = null

    override fun connect() {
        try {
            runBlocking {
                client.webSocket(host = host, port = port, path = path) {
                }
            }
        } catch (exception: Exception) {
            throw IllegalStateException("Failed to connect to ws://$host:$port$path", exception)
        }

        receiveJob = scope.launch {
            client.webSocket(host = host, port = port, path = path) {
                for (frame in incoming) {
                    onReceiveData.emit(frame.readBytes())
                }
            }
        }
    }

    override fun send(data: ByteArray) {
        runBlocking {
            client.webSocket(host = host, port = port, path = path) {
                send(data)
            }
        }
    }
}
