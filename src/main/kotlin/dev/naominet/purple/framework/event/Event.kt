package dev.naominet.purple.framework.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class Event<T> {
    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun listen(listener: (T) -> Unit): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun emit(data: T) {
        listeners.forEach {
            listener ->
            run {
                scope.launch {
                    listener(data)
                }
            }
        }
    }
}