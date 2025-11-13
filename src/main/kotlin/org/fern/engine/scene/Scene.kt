package org.fern.engine.scene

import org.fern.engine.scene.component.Component

class Scene {

    private val _roots = mutableListOf<GameObject>()
    private val allObjects = mutableSetOf<GameObject>()

    val roots get() = _roots.toList()

    fun createGameObject(name: String = "GameObject", parent: GameObject? = null): GameObject {
        val go = GameObject(name)
        attach(go, parent)
        return go
    }

    fun attach(go: GameObject, parent: GameObject? = null) {
        if (allObjects.contains(go)) {
            go.setParent(parent)
            return
        }

        go.attachToScene(this, parent)
        if (parent == null)
            _roots.add(go)
        allObjects.add(go)
    }

    fun destroy(go: GameObject) {
        if (!allObjects.contains(go)) return

        if (go.parent == null)
            _roots.remove(go)
        else
            go.detachFromParent()

        go.disposeRecursive()
        allObjects.remove(go)
    }

    fun addRootIfMissing(go: GameObject) {
        if (!allObjects.contains(go)) return

        if (!_roots.contains(go))
            _roots.add(go)
    }

    fun removeRootIfPresent(go: GameObject) {
        _roots.remove(go)
    }

    fun startIfNeeded() = _roots.forEach { it.startIfNeededRecursive() }

    fun update(dt: Float) {
        startIfNeeded()
        _roots.forEach { it.updateRecursive(dt) }
    }

    fun render() = _roots.forEach { it.renderRecursive() }

    fun dispose() {
        _roots.forEach { it.disposeRecursive() }
        _roots.clear()
        allObjects.clear()
    }

    fun findFirst(name: String) =
        allGameObjectsSequence().firstOrNull { it.name == name }

    inline fun <reified T : Component> findFirstComponent() =
        allGameObjectsSequence().firstNotNullOfOrNull { it.get<T>() }

    fun allGameObjectsSequence(): Sequence<GameObject> = sequence {
        val stack = ArrayDeque<GameObject>()
        _roots.asReversed().forEach { stack.addLast(it) }
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            yield(n)
            n.children.asReversed().forEach { stack.addLast(it) }
        }
    }

}