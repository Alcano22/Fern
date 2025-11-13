package org.fern.engine.scene.component

import org.fern.engine.editor.HideInInspector
import org.fern.engine.logger.logger
import org.fern.engine.renderer.IndexBuffer
import org.fern.engine.renderer.Renderer
import org.fern.engine.renderer.Shader
import org.fern.engine.renderer.Texture
import org.fern.engine.renderer.VertexArray
import org.fern.engine.renderer.VertexBuffer
import org.fern.engine.renderer.bufferLayout
import org.fern.engine.util.Color

@RegisterComponent("Rendering")
@SingleComponent
class SpriteRenderer : Component() {

    companion object {
        private val logger = logger()

        private val VERTICES = floatArrayOf(
            -0.5f, -0.5f,   0f, 0f,
             0.5f, -0.5f,   1f, 0f,
             0.5f,  0.5f,   1f, 1f,
            -0.5f,  0.5f,   0f, 1f
        )

        private val INDICES = intArrayOf(
            0, 1, 2,
            2, 3, 0
        )
    }

    var texture: Texture? = null
    var tint = Color.WHITE

    private lateinit var va: VertexArray
    private lateinit var vb: VertexBuffer
    private lateinit var ib: IndexBuffer
    private lateinit var shader: Shader

    override fun onAdded() {
        va = VertexArray.create()

        vb = VertexBuffer.create(VERTICES)
        vb.layout = bufferLayout {
            float2("a_Position")
            float2("a_UV")
        }
        va.addVertexBuffer(vb)

        ib = IndexBuffer.create(INDICES)
        va.indexBuffer = ib

        shader = Shader.create("user:shaders/default.glsl")
        shader.compile()
    }

    override fun render() {
        if (texture != null) {
            shader.setInt("u_Texture", 0)
            shader.setBool("u_UseTexture", true)
        } else
            shader.setBool("u_UseTexture", false)
        shader.setColor("u_Tint", tint)

        texture?.bind()
        Renderer.submit(shader, va, transform.toMatrix())
        Texture.unbind()
    }

    override fun onRemoved() {
        va.dispose()
        vb.dispose()
        ib.dispose()
    }

}