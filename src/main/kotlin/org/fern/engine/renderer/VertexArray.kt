package org.fern.engine.renderer

import org.fern.engine.renderer.opengl.GLVertexArray

interface VertexArray {

    companion object {
        fun create() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLVertexArray()
        }

        fun unbind() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLVertexArray.unbind()
        }
    }

    var indexBuffer: IndexBuffer?
    val hasIndexBuffer: Boolean

    val isBound: Boolean

    fun bind()

    fun addVertexBuffer(buffer: VertexBuffer)

    fun dispose()

    fun getId(): Int

}