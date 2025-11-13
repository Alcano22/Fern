package org.fern.engine.renderer

import org.joml.*

class Camera(
    size: Float,
    aspectRatio: Float,
    zNear: Float,
    zFar: Float
) {

    val position = Vector2f()

    var rotation = 0f
        set(value) {
            field = value
            markDirty()
        }

    var aspectRatio = aspectRatio
        set(value) {
            field = value
            recalculateProjection()
            markDirty()
        }

    var size = size
        set(value) {
            field = value
            recalculateProjection()
            markDirty()
        }

    var zNear = zNear
        set(value) {
            field = value
            recalculateProjection()
            markDirty()
        }

    var zFar = zFar
        set(value) {
            field = value
            recalculateProjection()
            markDirty()
        }

    private val _projMatrix = Matrix4f()
    private val _viewMatrix = Matrix4f()
    private val _viewProjMatrix = Matrix4f()

    val projMatrix get() = Matrix4f(_projMatrix)

    val viewMatrix: Matrix4f
        get() {
            updateCacheIfNeeded()
            return Matrix4f(_viewMatrix)
        }

    val viewProjMatrix: Matrix4f
        get() {
            updateCacheIfNeeded()
            return Matrix4f(_viewProjMatrix)
        }

    private var isCacheValid = false
    private val lastPosition = Vector2f()
    private var lastRotation = 0f

    init {
        recalculateProjection()
        markDirty()
    }

    fun set(size: Float, aspectRatio: Float, zNear: Float, zFar: Float) {
        this.size = size
        this.aspectRatio = aspectRatio
        this.zNear = zNear
        this.zFar = zFar
        recalculateProjection()
        markDirty()
    }

    private fun recalculateProjection() {
        val right = size * aspectRatio * 0.5f
        val top = size * 0.5f
        _projMatrix.identity()
            .ortho(-right, right, -top, top, zNear, zFar)

        markDirty()
    }

    private fun updateCacheIfNeeded() {
        if (
            isCacheValid &&
            lastPosition == position &&
            lastRotation == rotation
        ) return

        val rad = Math.toRadians(rotation)
        _viewMatrix.identity()
            .translate(position.x, position.y, 0f)
            .rotateZ(-rad)
            .invert()

        _projMatrix.mul(_viewMatrix, _viewProjMatrix)

        lastPosition.set(position)
        lastRotation = rotation
        isCacheValid = true
    }

    private fun markDirty() { isCacheValid = false }

}
