package org.fern.engine.renderer

import org.fern.engine.util.Color
import org.fern.engine.logger.logger
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f

enum class ClearMask {
    COLOR,
    DEPTH,
    STENCIL
}

object Renderer {

    private val logger = logger()

    private lateinit var api: RenderAPI

    private val viewProjMatrix = Matrix4f()

    val apiType get() = api.type

    fun init() {
        val apiTypeStr = System.getProperty("renderApi").trim()
        val apiType = RenderAPI.Type.valueOf(apiTypeStr)
        api = RenderAPI.create(apiType)
        api.init()

        logger.info { "Initialized (${apiType.displayName})" }
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) = api.setViewport(x, y, width, height)

    fun setClearColor(r: Float, g: Float, b: Float, a: Float = 1f) = api.setClearColor(r, g, b, a)
    fun setClearColor(color: Vector3f) = setClearColor(color.x, color.y, color.z)
    fun setClearColor(color: Vector4f) = setClearColor(color.x, color.y, color.z, color.w)
    fun setClearColor(color: Color) = setClearColor(color.r, color.g, color.b, color.a)

    fun clear(vararg masks: ClearMask) = api.clear(*masks)

    fun begin(camera: Camera) {
        viewProjMatrix.set(camera.viewProjMatrix)
    }

    fun end() {
        viewProjMatrix.identity()
    }

    fun submit(
        shader: Shader,
        vertexArray: VertexArray,
        transformMatrix: Matrix4f
    ) {
        val mvpMatrix = viewProjMatrix.mul(transformMatrix, Matrix4f())
        shader.setMat4("u_MVP", mvpMatrix)

        shader.bind()
        api.drawIndexed(vertexArray)
        Shader.unbind()
    }

}