package dev.naominet.purple.framework.event

import java.util.concurrent.CopyOnWriteArrayList

class Event<T> {
    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()

    fun listen(listener: (T) -> Unit): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }

    fun emit(data: T) {
        listeners.forEach { listener -> listener(data) }
    }
}