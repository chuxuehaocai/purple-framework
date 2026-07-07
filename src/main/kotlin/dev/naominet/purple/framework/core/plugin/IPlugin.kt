package dev.naominet.purple.framework.core.plugin

interface IPlugin {
    val info: PluginInfomation
    fun start(): IPlugin
}