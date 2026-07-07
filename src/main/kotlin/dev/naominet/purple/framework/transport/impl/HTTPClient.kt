package dev.naominet.purple.framework.transport.impl

import dev.naominet.purple.framework.event.Event
import dev.naominet.purple.framework.transport.ITransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.runBlocking

class HTTPClient(val host: String, val port: Int, val path: String) : ITransport {
    private val client = HttpClient(CIO)
    override val onReceiveData = Event<ByteArray>()

    override fun connect() {
        runBlocking {
            try {
                client.get("http://$host:$port$path")
            } catch (exception: Exception) {
                throw IllegalStateException("Failed to connect to http://$host:$port$path", exception)
            }
        }
    }

    override fun send(data: ByteArray) {
        runBlocking {
            val response = client.post("http://$host:$port$path") {
                setBody(data)
            }
            onReceiveData.emit(response.bodyAsBytes())
        }
    }
}
