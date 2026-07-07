package dev.naominet.purple.framework.core.plugin

import dev.naominet.purple.framework.core.PurpleFramework
import dev.naominet.purple.framework.embedded.EmbeddedPlugin
import dev.naominet.purple.framework.logger.Logger
import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.jar.JarFile

object PluginManager {
    val plugins = mutableListOf<IPlugin>()
    val pluginFolder = File("plugins")
    private val classLoaders = mutableListOf<URLClassLoader>()

    fun init(){
        plugins.clear()
        classLoaders.clear()

        if(PurpleFramework.configuration.embeddedPlugin){
            registerPlugin(EmbeddedPlugin(), "embedded")
        }

        if(!pluginFolder.exists()){
            pluginFolder.mkdirs()
            Logger.log("Plugin folder does not exist, created a new one: ${pluginFolder.absolutePath}", this.javaClass)
            return
        }

        val jars = pluginFolder.listFiles { file -> file.isFile && file.extension.equals("jar", true) } ?: emptyArray()
        if(jars.isEmpty()){
            Logger.log("No plugin jars found in ${pluginFolder.absolutePath}.", this.javaClass)
            return
        }

        for(jar in jars){
            loadJar(jar)
        }
    }

    private fun loadJar(jar: File){
        try{
            Logger.log("Loading plugin jar: ${jar.name}", this.javaClass)
            val classLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), this.javaClass.classLoader)
            classLoaders.add(classLoader)

            val servicePlugins = ServiceLoader.load(IPlugin::class.java, classLoader).toList()
            for(plugin in servicePlugins){
                registerPlugin(plugin, jar.name)
            }

            for(plugin in scanPluginClasses(jar, classLoader)){
                registerPlugin(plugin, jar.name)
            }
        }catch (throwable: Throwable){
            Logger.log("Failed to load plugin jar ${jar.name}: ${throwable.message}", this.javaClass)
        }
    }

    private fun scanPluginClasses(jar: File, classLoader: ClassLoader): List<IPlugin>{
        val plugins = mutableListOf<IPlugin>()

        JarFile(jar).use { jarFile ->
            for(entry in jarFile.entries().asSequence()){
                if(entry.isDirectory || !entry.name.endsWith(".class") || entry.name.contains("$")){
                    continue
                }

                val className = entry.name.removeSuffix(".class").replace('/', '.')
                val plugin = createPluginInstance(className, classLoader) ?: continue
                plugins.add(plugin)
            }
        }

        return plugins
    }

    private fun createPluginInstance(className: String, classLoader: ClassLoader): IPlugin?{
        return try{
            val clazz = Class.forName(className, false, classLoader)
            if(!IPlugin::class.java.isAssignableFrom(clazz) || clazz == IPlugin::class.java){
                return null
            }
            if(clazz.isInterface || Modifier.isAbstract(clazz.modifiers)){
                return null
            }

            val objectInstance = runCatching { clazz.getField("INSTANCE").get(null) }.getOrNull()
            if(objectInstance is IPlugin){
                objectInstance
            }else{
                val constructor = clazz.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance() as? IPlugin
            }
        }catch (_: ClassNotFoundException){
            null
        }catch (_: NoSuchMethodException){
            null
        }catch (throwable: Throwable){
            Logger.log("Failed to create plugin instance from $className: ${throwable.message}", this.javaClass)
            null
        }
    }

    private fun registerPlugin(plugin: IPlugin, source: String){
        try{
            if(plugins.any { it.info.id == plugin.info.id }){
                Logger.log("Skipped duplicate plugin: ${plugin.info.id} from $source", this.javaClass)
                return
            }

            val startedPlugin = plugin.start()
            plugins.add(startedPlugin)
            Logger.log("Loaded plugin: ${startedPlugin.info.id} from $source", this.javaClass)
        }catch (throwable: Throwable){
            Logger.log("Failed to start plugin from $source: ${throwable.message}", this.javaClass)
        }
    }
}