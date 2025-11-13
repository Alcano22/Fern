package org.fern.engine.scene.component

import org.joml.Math
import org.joml.Matrix4f
import org.joml.Vector2f

@SingleComponent
class Transform(
    val position: Vector2f = Vector2f(),
    var rotation: Float = 0f,
    val scale: Vector2f = Vector2f(1f)
) : Component() {

    private var isCacheValid = false
    private val cachedMatrix = Matrix4f()

    private val lastPosition = Vector2f()
    private var lastRotation = 0f
    private val lastScale = Vector2f(1f)

    fun toMatrix(): Matrix4f {
        updateCacheIfNeeded()
        return Matrix4f(cachedMatrix)
    }

    private fun updateCacheIfNeeded() {
        if (
            isCacheValid &&
            position == lastPosition &&
            rotation == lastRotation &&
            scale == lastScale
        ) return

        val rad = -Math.toRadians(rotation)
        cachedMatrix.identity()
            .translate(position.x, position.y, 0f)
            .rotateZ(rad)
            .scale(scale.x, scale.y, 1f)

        lastPosition.set(position)
        lastRotation = rotation
        lastScale.set(scale)

        isCacheValid = true
    }

    fun invalidateCache() { isCacheValid = false }

}