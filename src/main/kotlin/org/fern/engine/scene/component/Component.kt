package org.fern.engine.scene.component

import org.fern.engine.scene.GameObject

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SingleComponent

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ComponentName(val value: String)

abstract class Component {

    lateinit var gameObject: GameObject

    val scene get() = gameObject.scene
    val transform get() = gameObject.transform

    var enabled = true

    var started = false
        private set

    open fun onAdded() {}
    open fun start() {}

    open fun update(dt: Float) {}

    open fun render() {}

    open fun onRemoved() {}

    inline fun <reified T : Component> get() = gameObject.get<T>()
    inline fun <reified T : Component> getAll() = gameObject.getAll<T>()

    fun attachTo(go: GameObject) { gameObject = go }

    fun markStarted() { started = true }

}