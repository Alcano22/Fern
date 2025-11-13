package org.fern.engine.scene.component

import org.fern.engine.editor.HideInInspector
import org.fern.engine.editor.MinFloat
import org.fern.engine.editor.MinInt
import org.fern.engine.editor.Step
import org.fern.engine.renderer.Camera

@RegisterComponent("Rendering")
@SingleComponent
@ComponentName("Camera")
class CameraComponent : Component() {

    @HideInInspector
    val camera = Camera(20f, 16f / 9f, -1f, 1f)

    @MinInt(0)
    var targetDisplay = 0

    @MinFloat(0.01f)
    @Step(0.1f)
    var size: Float
        get() = camera.size
        set(value) { camera.size = value }

    var zNear: Float
        get() = camera.zNear
        set(value) { camera.zNear = value }

    var zFar: Float
        get() = camera.zFar
        set(value) { camera.zFar = value }

    override fun update(dt: Float) {
        camera.position.set(transform.position)
        camera.rotation = transform.rotation
    }

}