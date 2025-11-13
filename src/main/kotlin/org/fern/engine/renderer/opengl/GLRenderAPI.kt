package org.fern.engine.renderer.opengl

import org.fern.engine.renderer.ClearMask
import org.fern.engine.renderer.RenderAPI
import org.fern.engine.renderer.VertexArray
import org.fern.engine.util.nullptr
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL45C.*

class GLRenderAPI : RenderAPI {

    override val type = RenderAPI.Type.OPEN_GL

    override fun init() {
        GL.createCapabilities()

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) = glViewport(x, y, width, height)

    override fun setClearColor(r: Float, g: Float, b: Float, a: Float) = glClearColor(r, g, b, a)

    override fun clear(vararg masks: ClearMask) {
        var bits = 0
        for (mask in masks)
            bits = bits or clearMaskToOpenGLBit(mask)
        glClear(bits)
    }

    private fun clearMaskToOpenGLBit(mask: ClearMask) =
        when (mask) {
            ClearMask.COLOR   -> GL_COLOR_BUFFER_BIT
            ClearMask.DEPTH   -> GL_DEPTH_BUFFER_BIT
            ClearMask.STENCIL -> GL_STENCIL_BUFFER_BIT
        }

    override fun drawIndexed(vertexArray: VertexArray) {
        if (!vertexArray.hasIndexBuffer) return

        vertexArray.bind()
        glDrawElements(GL_TRIANGLES, vertexArray.indexBuffer!!.count, GL_UNSIGNED_INT, nullptr)
        VertexArray.unbind()
    }

}