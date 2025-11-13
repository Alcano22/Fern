package org.fern.engine.scene

import org.fern.engine.scene.component.Component
import org.fern.engine.scene.component.SingleComponent
import org.fern.engine.scene.component.Transform
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

class GameObject(var name: String = "GameObject") {

    lateinit var scene: Scene
        private set

    var active = true

    var parent: GameObject? = null
        private set

    val children = mutableListOf<GameObject>()

    private val components = mutableListOf<Component>()

    val transform: Transform
        get() = components[0] as Transform

    init {
        add(Transform())
    }

    fun <T : Component> add(component: T): T {
        val cls = component::class
        if (cls.hasAnnotation<SingleComponent>()) {
            val existing = components.firstOrNull { cls.isInstance(it) }
            require(existing == null) {
                "Component ${cls.simpleName} can only exist once per GameObject"
            }
        }

        if (component is Transform)
            components.add(0, component)
        else
            components.add(component)

        component.attachTo(this)
        component.onAdded()
        return component
    }

    fun remove(component: Component): Boolean {
        if (component === components.firstOrNull())
            return false

        val removed = components.remove(component)
        if (removed)
            component.onRemoved()
        return removed
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Component> get(cls: KClass<T>) =
        components.firstOrNull { cls.isInstance(it) } as T?

    @Suppress("UNCHECKED_CAST")
    fun <T : Component> getAll(cls: KClass<T>) =
        components.filter { cls.isInstance(it) }.map { it as T }

    fun <T : Component> removeFirst(cls: KClass<T>): Boolean {
        val comp = get(cls) ?: return false
        return remove(comp)
    }

    inline fun <reified T : Component> get() = get(T::class)
    inline fun <reified T : Component> getAll() = getAll(T::class)
    inline fun <reified T : Component> removeFirst() = removeFirst(T::class)

    fun hasComponents() = components.isNotEmpty()
    fun componentCount() = components.size

    fun addChild(child: GameObject): GameObject {
        scene.attach(child, this)
        return child
    }

    fun setParent(newParent: GameObject?) {
        if (parent === newParent) return

        parent?.children?.remove(this)
        parent = newParent

        if (newParent != null) {
            if (!newParent.children.contains(this))
                newParent.children.add(this)
            scene.removeRootIfPresent(this)
        } else
            scene.addRootIfMissing(this)
    }

    fun startIfNeededRecursive() {
        if (!active) return

        for (comp in components) {
            if (!comp.enabled || comp.started) continue

            comp.start()
            comp.markStarted()
        }
        for (child in children)
            child.startIfNeededRecursive()
    }

    fun updateRecursive(dt: Float) {
        if (!active) return

        for (comp in components) {
            if (comp.enabled)
                comp.update(dt)
        }
        for (child in children)
            child.updateRecursive(dt)
    }

    fun renderRecursive() {
        if (!active) return

        for (comp in components) {
            if (comp.enabled)
                comp.render()
        }
        for (child in children)
            child.renderRecursive()
    }

    fun disposeRecursive() {
        for (child in children)
            child.disposeRecursive()
        children.clear()

        for (comp in components)
            comp.onRemoved()
        components.clear()
    }

    fun attachToScene(scene: Scene, parent: GameObject?) {
        this.scene = scene
        this.parent = parent
        parent?.children?.add(this)
    }

    fun detachFromParent() {
        parent?.children?.remove(this)
        parent = null
    }

}