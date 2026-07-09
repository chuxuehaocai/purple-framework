# Purple Framework

Purple Framework 是一个使用 Kotlin/JVM 编写的机器人框架，当前主要面向 OneBot 风格的消息 JSON。框架负责加载配置、初始化插件、建立 HTTP/WebSocket 传输连接，并把收到的消息分发到事件系统中。

## 特性

- 支持的传输协议：
  - `WebsocketClient`
  - `WebsocketServer`
- 基于事件的消息分发
- 插件系统：
  - 内置插件
  - `plugins/` 目录外部 jar 插件
  - `ServiceLoader<IPlugin>` 插件发现
  - 自动扫描实现 `IPlugin` 的 class
- 自动生成并补全配置文件
- 简单的消息构建工具 `MessageBuilder`

## 环境要求

- JDK 21
- Gradle Wrapper 已包含在项目中，无需单独安装 Gradle

## 构建

Windows PowerShell：

```powershell
./gradlew.bat build
```

Linux / macOS / Git Bash：

```bash
./gradlew build
```

仅编译 Kotlin：

```bash
./gradlew compileKotlin
```

运行测试：

```bash
./gradlew test
```

清理构建产物：

```bash
./gradlew clean
```

## 运行

项目入口位于：

```text
src/main/kotlin/dev/naominet/purple/framework/EntryPoint.kt
```

主函数会调用：

```kotlin
PurpleFramework.start(args)
```

当前 `build.gradle.kts` 尚未配置 `application` 插件，因此如果需要直接通过 Gradle 运行，可以后续添加：

```kotlin
plugins {
    application
}

application {
    mainClass.set("dev.naominet.purple.framework.EntryPointKt")
}
```

之后可使用：

```bash
./gradlew run
```

## 配置

首次启动时，框架会自动创建配置目录和配置文件：

```text
configs/purple-framework-config.json
```

配置模型位于：

```text
src/main/kotlin/dev/naominet/purple/framework/core/FrameConfig.kt
```

默认配置项包括：

```kotlin
val protocol: String = "HTTPServer"
val address: String = "127.0.0.1"
val port: Int = 8080
val path: String = "/"
val encoding: String = "UTF-8"
val debugOutput: Boolean = false
val embeddedPlugin: Boolean = true
```

`protocol` 支持以下值：

- `HTTPClient`
- `HTTPServer`
- `WebsocketServer`
- `WebsocketClient`

## 架构概览

启动流程：

1. `EntryPoint.kt` 调用 `PurpleFramework.start(args)`
2. `ConfigManager.register(FrameConfig())` 加载或创建框架配置
3. `PurpleFramework.checkConfiguration()` 校验配置合法性
4. `PluginManager.init()` 加载内置插件和外部插件
5. 根据配置创建对应的 `ITransport` 实现
6. 调用 `transport.connect()` 建立连接或启动服务
7. `originEventListener()` 监听传输层收到的数据
8. 收到的 JSON 被解析为 `TextMessageBean`
9. 根据 `message_type` 分发到 `EventManager.groupMessageEvent` 或 `EventManager.privateMessageEvent`

## 事件系统

事件类位于：

```text
src/main/kotlin/dev/naominet/purple/framework/event/Event.kt
```

使用方式：

```kotlin
EventManager.groupMessageEvent.listen { msg ->
    // handle group message
}
```

`listen` 会返回一个 `AutoCloseable`，可用于取消监听。

## 发送消息

可以通过 `Bot.sendGroupMessage` 发送群消息：

```kotlin
Bot.sendGroupMessage(groupId = 123456L, msg = "Hello Purple!")
```

也可以使用 `MessageBuilder` 构建 CQ 码消息：

```kotlin
val msg = MessageBuilder()
    .at(123456L)
    .append(" hello")
    .toString()

Bot.sendGroupMessage(10000L, msg)
```

## 插件开发

插件需要实现 `IPlugin`：

```kotlin
import dev.naominet.purple.framework.core.plugin.IPlugin
import dev.naominet.purple.framework.core.plugin.PluginInfomation

class MyPlugin : IPlugin {
    override val info = PluginInfomation("my-plugin")

    override fun start(): IPlugin {
        // register listeners here
        return this
    }
}
```

示例监听群消息：

```kotlin
class MyPlugin : IPlugin {
    override val info = PluginInfomation("my-plugin")

    override fun start(): IPlugin {
        EventManager.groupMessageEvent.listen { msg ->
            if (msg.raw_message == "ping") {
                Bot.sendGroupMessage(msg.group_id, "pong")
            }
        }
        return this
    }
}
```

将插件打包成 jar 后放入运行目录下的：

```text
plugins/
```

框架启动时会自动加载其中的 `.jar` 文件。

插件发现方式：

1. 推荐：使用 `ServiceLoader`，在插件 jar 中提供：

```text
META-INF/services/dev.naominet.purple.framework.core.plugin.IPlugin
```

文件内容为插件类全限定名，例如：

```text
com.example.MyPlugin
```

2. 兜底：框架会扫描 jar 中实现了 `IPlugin` 的具体类，并尝试通过无参构造或 Kotlin `object` 的 `INSTANCE` 创建插件实例。

插件 ID 来自 `plugin.info.id`，重复 ID 会被跳过。

## 内置插件

内置插件位于：

```text
src/main/kotlin/dev/naominet/purple/framework/embedded/EmbeddedPlugin.kt
```

当配置项 `embeddedPlugin` 为 `true` 时会加载。当前内置插件会监听群消息，并对部分命令返回框架状态信息。