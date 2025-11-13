package org.fern.engine.event

import kotlin.reflect.KClass

sealed class Event {
    val name get() = this::class.simpleName ?: "Event"
}

object EventManager {
    private val callbacks: MutableMap<KClass<out Event>, MutableList<(Event) -> Unit>> = mutableMapOf()

    fun <T : Event> register(eventClass: KClass<T>, callback: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val wrapper: (Event) -> Unit = { e -> (e as? T)?.let(callback) }
        callbacks.getOrPut(eventClass) { mutableListOf() }.add(wrapper)
    }

    inline fun <reified T : Event> addCallback(noinline callback: (T) -> Unit) = register(T::class, callback)

    fun dispatch(event: Event) =
        callbacks[event::class]?.forEach { it(event) }

    fun clear() = callbacks.clear()
}
