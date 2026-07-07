package dev.naominet.purple.framework.transport.impl

import dev.naominet.purple.framework.event.Event
import dev.naominet.purple.framework.logger.Logger
import dev.naominet.purple.framework.transport.ITransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
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
    @Volatile
    private var session: DefaultClientWebSocketSession? = null

    override fun connect() {
        val connected = CompletableDeferred<Unit>()

        receiveJob = scope.launch {
            try {
                client.webSocket(host = host, port = port, path = path) {
                    session = this
                    connected.complete(Unit)
                    Logger.log("Connected to ws://$host:$port$path", this.javaClass)

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> onReceiveData.emit(frame.readText().toByteArray())
                            else -> Unit
                        }
                    }
                }
            }catch (exception: Exception) {
                session = null
                val wrapped = IllegalStateException("Failed to connect to ws://$host:$port$path", exception)
                if (!connected.isCompleted) {
                    connected.completeExceptionally(wrapped)
                } else {
                    Logger.log(wrapped.message ?: "WebSocket client disconnected.", this.javaClass)
                }
            } finally {
                session = null
            }
        }

        runBlocking {
            connected.await()
        }
    }

    override fun send(data: ByteArray) {
        val currentSession = session ?: throw IllegalStateException("WebSocket client is not connected to ws://$host:$port$path")
        runBlocking {
            currentSession.send(data)
        }
    }
}
