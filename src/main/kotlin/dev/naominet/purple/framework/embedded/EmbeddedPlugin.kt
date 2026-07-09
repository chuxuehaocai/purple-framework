package dev.naominet.purple.framework.embedded

import dev.naominet.purple.framework.beans.TextMessageBean
import dev.naominet.purple.framework.core.Bot
import dev.naominet.purple.framework.core.plugin.IPlugin
import dev.naominet.purple.framework.core.plugin.PluginInfomation
import dev.naominet.purple.framework.event.EventManager
import dev.naominet.purple.framework.utils.MessageBuilder
import java.lang.management.ManagementFactory

class EmbeddedPlugin: IPlugin {
    override val info: PluginInfomation
        get() = PluginInfomation("framework-embedded-plugin")

    override fun start(): IPlugin {
        EventManager.groupMessageEvent.listen { event -> listener(event) }
        return this
    }

    fun formatBytes(bytes: Long): String {
        return "%.2f MB".format(bytes / 1024.0 / 1024.0)
    }
    val runtime = Runtime.getRuntime()

    fun listener(msg: TextMessageBean){
        if(msg.raw_message == "#purple"){
            val used = runtime.totalMemory() - runtime.freeMemory()
            val total = runtime.totalMemory()
            val max = runtime.maxMemory()
            val sb = MessageBuilder()
            sb.replyGroup(msg.message_id,"Purple Framework Information:\n")
            sb.append("JVM Memory:\n")
            sb.append("Used: ${formatBytes(used)} / ${formatBytes(max)} (%.2f%%)".format(used * 100.0 / max)+"\n")
            sb.append("Heap: ${formatBytes(used)} / ${formatBytes(total)} (%.2f%%)".format(used * 100.0 / total)+"\n")

            val bean = ManagementFactory.getOperatingSystemMXBean()
                    as com.sun.management.OperatingSystemMXBean
            sb.append("CPU Load: ${"%.1f".format(bean.cpuLoad * 100)}%"+"\n")
            sb.toString().lineSequence().maxByOrNull { it.length }?.let {
                for(i in it.indices){
                    sb.append("--")
                }
            }
            sb.append("\n")
            sb.append("Running on ${System.getProperty("os.name")}, ${System.getProperty("os.arch")}"+"\n")
            Bot.sendGroupMessage(msg.group_id, sb.toString())
        }
    }
}
