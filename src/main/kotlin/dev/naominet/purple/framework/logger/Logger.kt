package dev.naominet.purple.framework.logger

import dev.naominet.purple.framework.core.PurpleFramework
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    fun log(msg : String, source: Class<Any>) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val time = LocalDateTime.now().format(formatter)
        println("[$time][${source.name.replace('/', '.')}] $msg")
    }

    fun debug(msg : String, source: Class<Any>) {
        if(PurpleFramework.configuration.debugOutput) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val time = LocalDateTime.now().format(formatter)
            println("[DEBUG][$time][${source.name.replace('/', '.')}] $msg")
        }
    }
}