# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

This is a Gradle Kotlin/JVM project using the checked-in Gradle wrapper.

- Build and compile main sources: `./gradlew build`
- Compile Kotlin only: `./gradlew compileKotlin`
- Run tests: `./gradlew test`
- Run a single test class or method: `./gradlew test --tests "fully.qualified.TestName"` or `./gradlew test --tests "fully.qualified.TestName.methodName"`
- Clean build output: `./gradlew clean`

On Windows PowerShell, use `./gradlew.bat ...` instead of `./gradlew ...`.

No lint/formatting task is configured in `build.gradle.kts` at the moment.

## Architecture overview

Purple Framework is a Kotlin bot framework centered around `PurpleFramework.start()` in `src/main/kotlin/dev/naominet/purple/framework/core/PurpleFramework.kt`.

Startup flow:

1. `EntryPoint.kt` calls `PurpleFramework.start(args)`.
2. `ConfigManager.register(FrameConfig())` loads or creates `configs/purple-framework-config.json`.
3. `PurpleFramework.checkConfiguration()` validates protocol, address/port, path, and encoding.
4. `PluginManager.init()` loads the embedded plugin and external plugin jars.
5. The configured transport is constructed and connected.
6. `originEventListener()` listens to raw transport bytes, parses JSON into `TextMessageBean`, then emits group/private message events through `EventManager`.

## Configuration

`FrameConfig` is the framework config model. It controls:

- `protocol`: one of `HTTPClient`, `HTTPServer`, `WebsocketServer`, `WebsocketClient`
- `address`, `port`, `path`: transport connection settings
- `encoding`: used when converting received/sent bytes to text
- `debugOutput`: enables `Logger.debug`
- `embeddedPlugin`: controls whether the built-in plugin is loaded

`ConfigManager` writes missing config files with defaults and fills missing keys in existing JSON configs.

## Events and messages

The event system is intentionally small:

- `Event<T>` stores listeners in a `CopyOnWriteArrayList` and returns `AutoCloseable` handles from `listen`.
- `EventManager` currently exposes `groupMessageEvent` and `privateMessageEvent` for `TextMessageBean`.
- Incoming raw JSON is parsed into `TextMessageBean`, which models OneBot-like message fields including `message_type`, `raw_message`, `group_id`, `sender`, and message segments.

Outgoing group messages go through `Bot.sendGroupMessage()`, which builds an action JSON payload and sends it through the active `PurpleFramework.transport` using the configured encoding.

## Transports

All transports implement `ITransport`:

```kotlin
interface ITransport {
    val onReceiveData: Event<ByteArray>
    fun connect()
    fun send(data: ByteArray)
}
```

Transport implementations live under `transport/impl` and use Ktor:

- HTTP server/client implementations for HTTP mode
- WebSocket server/client implementations for WebSocket mode

When changing transport behavior, preserve the contract that received bytes are emitted via `onReceiveData` and outgoing bytes are sent via `send`.

## Plugins

Plugins implement `core/plugin/IPlugin.kt`:

```kotlin
interface IPlugin {
    val info: PluginInfomation
    fun start(): IPlugin
}
```

`PluginManager` loads plugins from the relative `plugins/` directory. It supports:

- the built-in `EmbeddedPlugin` when `FrameConfig.embeddedPlugin` is true
- external `.jar` files in `plugins/`
- `ServiceLoader<IPlugin>` discovery via `META-INF/services/dev.naominet.purple.framework.core.plugin.IPlugin`
- fallback class scanning for concrete `IPlugin` implementations, including Kotlin `object` plugins through their `INSTANCE` field

Plugin IDs (`plugin.info.id`) are used to skip duplicates. Plugin startup failures are logged and should not prevent other plugins from loading.

The built-in `EmbeddedPlugin` registers a group message listener and handles framework status/debug responses using `MessageBuilder` and `Bot.sendGroupMessage()`.
