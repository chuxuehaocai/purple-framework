package dev.naominet.purple.framework.core.plugin

import dev.naominet.purple.framework.core.PurpleFramework
import dev.naominet.purple.framework.embedded.EmbeddedPlugin
import dev.naominet.purple.framework.logger.Logger
import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

object PluginManager {
    data class PluginState(
        val id: String,
        val source: String,
        val enabled: Boolean,
        val embedded: Boolean,
        val lastError: String? = null,
    )

    private data class PluginRecord(
        val id: String,
        val source: String,
        val embedded: Boolean,
        var instance: IPlugin? = null,
        var classLoader: URLClassLoader? = null,
        var lastError: String? = null,
    )

    val plugins = mutableListOf<IPlugin>()
    val pluginFolder = File("plugins")
    private val records = linkedMapOf<String, PluginRecord>()
    private val disabledPlugins = ConcurrentHashMap.newKeySet<String>()
    private val lock = Any()

    fun init() = synchronized(lock) {
        stopAllLocked()
        records.clear()
        plugins.clear()

        if (PurpleFramework.configuration.embeddedPlugin) {
            val record = PluginRecord("framework-embedded-plugin", "embedded", true)
            records[record.id] = record
            if (!disabledPlugins.contains(record.id)) {
                startRecord(record, EmbeddedPlugin(), null)
            }
        }

        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs()
            Logger.log("Plugin folder does not exist, created a new one: ${pluginFolder.absolutePath}", this.javaClass)
            return@synchronized
        }

        val jars = pluginFolder.listFiles { file -> file.isFile && file.extension.equals("jar", true) }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()
        if (jars.isEmpty()) {
            Logger.log("No plugin jars found in ${pluginFolder.absolutePath}.", this.javaClass)
            return@synchronized
        }

        jars.forEach(::discoverJar)
    }

    fun states(): List<PluginState> = synchronized(lock) {
        records.values.map { record ->
            PluginState(
                id = record.id,
                source = record.source,
                enabled = record.instance != null,
                embedded = record.embedded,
                lastError = record.lastError,
            )
        }
    }

    fun enable(id: String): PluginState = synchronized(lock) {
        val record = records[id] ?: throw IllegalArgumentException("Unknown plugin: $id")
        if (record.instance == null) {
            val plugin = if (record.embedded) EmbeddedPlugin() else instantiateFromJar(record)
            startRecord(record, plugin, record.classLoader)
        }
        disabledPlugins.remove(id)
        stateOf(record)
    }

    fun disable(id: String): PluginState = synchronized(lock) {
        val record = records[id] ?: throw IllegalArgumentException("Unknown plugin: $id")
        val plugin = record.instance
        if (plugin != null) {
            plugin.stop()
            plugins.remove(plugin)
            record.instance = null
            record.classLoader?.close()
            record.classLoader = null
            Logger.log("Disabled plugin: $id", this.javaClass)
        }
        disabledPlugins.add(id)
        record.lastError = null
        stateOf(record)
    }

    private fun discoverJar(jar: File) {
        var classLoader: URLClassLoader? = null
        try {
            Logger.log("Discovering plugin jar: ${jar.name}", this.javaClass)
            classLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), this.javaClass.classLoader)
            val candidates = linkedMapOf<String, IPlugin>()

            ServiceLoader.load(IPlugin::class.java, classLoader).forEach { plugin ->
                candidates.putIfAbsent(plugin.info.id, plugin)
            }
            scanPluginClasses(jar, classLoader).forEach { plugin ->
                candidates.putIfAbsent(plugin.info.id, plugin)
            }

            if (candidates.isEmpty()) {
                classLoader.close()
                return
            }

            var loaderOwned = false
            candidates.values.forEach { plugin ->
                if (records.containsKey(plugin.info.id)) {
                    Logger.log("Skipped duplicate plugin: ${plugin.info.id} from ${jar.name}", this.javaClass)
                    return@forEach
                }
                val record = PluginRecord(plugin.info.id, jar.name, false)
                records[record.id] = record
                if (!disabledPlugins.contains(record.id)) {
                    startRecord(record, plugin, classLoader)
                    loaderOwned = true
                }
            }
            if (!loaderOwned) classLoader.close()
        } catch (throwable: Throwable) {
            runCatching { classLoader?.close() }
            Logger.log("Failed to discover plugin jar ${jar.name}: ${throwable.message}", this.javaClass)
        }
    }

    private fun instantiateFromJar(record: PluginRecord): IPlugin {
        val jar = File(pluginFolder, record.source)
        require(jar.isFile) { "Plugin jar no longer exists: ${record.source}" }
        val classLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), this.javaClass.classLoader)
        record.classLoader = classLoader
        val plugin = ServiceLoader.load(IPlugin::class.java, classLoader)
            .firstOrNull { it.info.id == record.id }
            ?: scanPluginClasses(jar, classLoader).firstOrNull { it.info.id == record.id }
        return plugin ?: run {
            classLoader.close()
            record.classLoader = null
            throw IllegalStateException("Plugin ${record.id} was not found in ${record.source}")
        }
    }

    private fun startRecord(record: PluginRecord, plugin: IPlugin, classLoader: URLClassLoader?) {
        try {
            val started = plugin.start()
            record.instance = started
            record.classLoader = classLoader
            record.lastError = null
            plugins.add(started)
            Logger.log("Enabled plugin: ${record.id} from ${record.source}", this.javaClass)
        } catch (throwable: Throwable) {
            record.instance = null
            record.lastError = throwable.message ?: throwable.javaClass.simpleName
            runCatching { classLoader?.close() }
            record.classLoader = null
            Logger.log("Failed to enable plugin ${record.id}: ${record.lastError}", this.javaClass)
            throw throwable
        }
    }

    private fun stopAllLocked() {
        records.values.forEach { record ->
            runCatching { record.instance?.stop() }
            runCatching { record.classLoader?.close() }
        }
    }

    private fun stateOf(record: PluginRecord) = PluginState(
        id = record.id,
        source = record.source,
        enabled = record.instance != null,
        embedded = record.embedded,
        lastError = record.lastError,
    )

    private fun scanPluginClasses(jar: File, classLoader: ClassLoader): List<IPlugin> {
        val discovered = mutableListOf<IPlugin>()
        JarFile(jar).use { jarFile ->
            for (entry in jarFile.entries().asSequence()) {
                if (entry.isDirectory || !entry.name.endsWith(".class") || entry.name.contains("$")) continue
                val className = entry.name.removeSuffix(".class").replace('/', '.')
                createPluginInstance(className, classLoader)?.let(discovered::add)
            }
        }
        return discovered
    }

    private fun createPluginInstance(className: String, classLoader: ClassLoader): IPlugin? = try {
        val clazz = Class.forName(className, false, classLoader)
        if (!IPlugin::class.java.isAssignableFrom(clazz) || clazz == IPlugin::class.java || clazz.isInterface || Modifier.isAbstract(clazz.modifiers)) {
            null
        } else {
            val objectInstance = runCatching { clazz.getField("INSTANCE").get(null) }.getOrNull()
            if (objectInstance is IPlugin) objectInstance else {
                val constructor = clazz.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance() as? IPlugin
            }
        }
    } catch (_: ClassNotFoundException) {
        null
    } catch (_: NoSuchMethodException) {
        null
    } catch (throwable: Throwable) {
        Logger.log("Failed to create plugin instance from $className: ${throwable.message}", this.javaClass)
        null
    }
}
