package dev.naominet.purple.framework.panel

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.naominet.purple.framework.core.PurpleFramework
import dev.naominet.purple.framework.core.plugin.PluginManager
import dev.naominet.purple.framework.logger.Logger
import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.Executors
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object FrameworkPanel {
    private val startedAt = System.currentTimeMillis()
    private var server: HttpServer? = null
    private var executor = newExecutor()

    fun start(port: Int) {
        if (server != null) return
        require(port in 1..65535) { "Panel port must be between 1 and 65535." }
        if (executor.isShutdown) executor = newExecutor()

        val created = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0)
        created.executor = executor
        created.createContext("/") { exchange -> handle(exchange) }
        created.start()
        server = created
        Logger.log("Framework Panel is available at http://127.0.0.1:$port/", this.javaClass)
    }

    fun stop() {
        server?.stop(1)
        server = null
        executor.shutdownNow()
    }

    private fun handle(exchange: HttpExchange) {
        try {
            securityHeaders(exchange)
            if (exchange.remoteAddress.address?.isLoopbackAddress != true) {
                sendJson(exchange, 403, jsonOf("error" to "loopback_only"))
                return
            }

            val path = exchange.requestURI.path
            when {
                exchange.requestMethod == "GET" && path == "/" -> sendResource(exchange, "/panel/index.html", "text/html; charset=utf-8")
                exchange.requestMethod == "GET" && (path == "/plugins" || path == "/plugins/") -> sendResource(exchange, "/panel/plugins.html", "text/html; charset=utf-8")
                exchange.requestMethod == "GET" && path == "/panel.css" -> sendResource(exchange, "/panel/panel.css", "text/css; charset=utf-8")
                exchange.requestMethod == "GET" && path == "/panel.js" -> sendResource(exchange, "/panel/panel.js", "text/javascript; charset=utf-8")
                exchange.requestMethod == "GET" && path == "/loop" -> sendResource(exchange, "/panel/loop.html", "text/html; charset=utf-8")
                exchange.requestMethod == "GET" && path == "/api/status" -> sendJson(exchange, 200, status())
                exchange.requestMethod == "GET" && path == "/api/plugins" -> sendJson(exchange, 200, plugins())
                exchange.requestMethod == "POST" && path.startsWith("/api/plugins/") -> mutatePlugin(exchange, path)
                path == "/api/plugins" || path.startsWith("/api/plugins/") -> methodNotAllowed(exchange)
                else -> sendJson(exchange, 404, jsonOf("error" to "not_found"))
            }
        } catch (error: IllegalArgumentException) {
            sendJsonSafely(exchange, 400, jsonOf("error" to "bad_request", "message" to error.message))
        } catch (error: Exception) {
            Logger.log("Panel request failed: ${error.message}", this.javaClass)
            sendJsonSafely(exchange, 500, jsonOf("error" to "internal_error", "message" to (error.message ?: "Unknown error")))
        } finally {
            exchange.close()
        }
    }

    private fun mutatePlugin(exchange: HttpExchange, path: String) {
        require(exchange.requestHeaders.getFirst("X-Panel-Request") == "plugin-state") { "Missing panel request header." }
        val origin = exchange.requestHeaders.getFirst("Origin")
        if (origin != null) {
            val expected = "http://${exchange.localAddress.hostString}:${exchange.localAddress.port}"
            require(origin == expected || origin == "http://localhost:${exchange.localAddress.port}") { "Cross-origin request rejected." }
        }

        val match = Regex("^/api/plugins/(.+)/(enable|disable)$").matchEntire(path)
            ?: throw IllegalArgumentException("Unknown plugin action.")
        val id = URLDecoder.decode(match.groupValues[1], StandardCharsets.UTF_8)
        val state = when (match.groupValues[2]) {
            "enable" -> PluginManager.enable(id)
            else -> PluginManager.disable(id)
        }
        sendJson(exchange, 200, pluginJson(state))
    }

    private fun status(): JsonObject {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val cpu = (os as? com.sun.management.OperatingSystemMXBean)?.cpuLoad?.takeIf { it >= 0.0 }
        val transportReady = runCatching { PurpleFramework.transport }.isSuccess
        return jsonOf(
            "frameworkVersion" to PurpleFramework.version,
            "uptimeMillis" to (System.currentTimeMillis() - startedAt),
            "memoryUsedBytes" to usedMemory,
            "memoryMaxBytes" to runtime.maxMemory(),
            "cpuLoad" to cpu,
            "processors" to runtime.availableProcessors(),
            "protocol" to PurpleFramework.configuration.protocol,
            "transportReady" to transportReady,
            "pluginCount" to PluginManager.states().size,
            "enabledPluginCount" to PluginManager.states().count { it.enabled },
            "observedAt" to Instant.now().toString(),
        )
    }

    private fun plugins(): JsonObject = jsonOf(
        "plugins" to PluginManager.states().map(::pluginJson),
        "observedAt" to Instant.now().toString(),
    )

    private fun pluginJson(state: PluginManager.PluginState): JsonObject = jsonOf(
        "id" to state.id,
        "source" to state.source,
        "enabled" to state.enabled,
        "embedded" to state.embedded,
        "lastError" to state.lastError,
    )

    private fun sendResource(exchange: HttpExchange, path: String, contentType: String) {
        val bytes = FrameworkPanel::class.java.getResourceAsStream(path)?.use { it.readBytes() }
            ?: run {
                sendJson(exchange, 404, jsonOf("error" to "resource_not_found"))
                return
            }
        send(exchange, 200, contentType, bytes)
    }

    private fun securityHeaders(exchange: HttpExchange) {
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.responseHeaders.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
        exchange.responseHeaders.set("Referrer-Policy", "no-referrer")
        exchange.responseHeaders.set("X-Content-Type-Options", "nosniff")
        exchange.responseHeaders.set("X-Frame-Options", "DENY")
    }

    private fun methodNotAllowed(exchange: HttpExchange) {
        exchange.responseHeaders.set("Allow", "GET, POST")
        sendJson(exchange, 405, jsonOf("error" to "method_not_allowed"))
    }

    private fun sendJson(exchange: HttpExchange, status: Int, body: JsonObject) =
        send(exchange, status, "application/json; charset=utf-8", Json.encodeToString(body).toByteArray(StandardCharsets.UTF_8))

    private fun sendJsonSafely(exchange: HttpExchange, status: Int, body: JsonObject) {
        runCatching { sendJson(exchange, status, body) }
    }

    private fun send(exchange: HttpExchange, status: Int, contentType: String, body: ByteArray) {
        exchange.responseHeaders.set("Content-Type", contentType)
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }

    private fun jsonOf(vararg values: Pair<String, Any?>): JsonObject = buildJsonObject {
        values.forEach { (key, value) -> put(key, value.toJsonElement()) }
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        else -> JsonPrimitive(toString())
    }

    private fun newExecutor() = Executors.newFixedThreadPool(4) { task ->
        Thread(task, "purple-panel").apply { isDaemon = true }
    }
}
