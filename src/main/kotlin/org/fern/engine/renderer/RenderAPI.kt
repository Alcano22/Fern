package org.fern.engine.renderer

import org.fern.engine.renderer.opengl.GLRenderAPI

interface RenderAPI {

    enum class Type(val displayName: String, val factory: () -> RenderAPI) {
        OPEN_GL("OpenGL", ::GLRenderAPI)
    }

    companion object {
        fun create(type: Type) = type.factory()
    }

    val type: Type

    fun init()

    fun setViewport(x: Int, y: Int, width: Int, height: Int)

    fun setClearColor(r: Float, g: Float, b: Float, a: Float)

    fun clear(vararg masks: ClearMask)

    fun drawIndexed(vertexArray: VertexArray)

}