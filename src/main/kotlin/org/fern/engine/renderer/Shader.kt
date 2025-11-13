package org.fern.engine.renderer

import org.fern.engine.renderer.opengl.GLShader
import org.fern.engine.util.Color
import org.joml.*

interface Shader {

    companion object {
        fun create(
            name: String,
            vertexSrc: String,
            fragmentSrc: String
        ): Shader = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLShader(name, vertexSrc, fragmentSrc)
        }

        fun create(filepath: String): Shader = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLShader(filepath)
        }

        fun unbind() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLShader.unbind()
        }
    }

    val isBound: Boolean

    fun compile()

    fun bind()

    fun setBool(name: String, value: Boolean) { setInt(name, if (value) 1 else 0) }
    fun setInt(name: String, value: Int)
    fun setFloat(name: String, value: Float)

    fun setInt2(name: String, x: Int, y: Int) = setInts(name, intArrayOf(x, y))
    fun setInt2(name: String, value: Vector2i) = setInt2(name, value.x, value.y)

    fun setInt3(name: String, x: Int, y: Int, z: Int) = setInts(name, intArrayOf(x, y, z))
    fun setInt3(name: String, value: Vector3i) = setInt3(name, value.x, value.y, value.z)

    fun setInt4(name: String, x: Int, y: Int, z: Int, w: Int) = setInts(name, intArrayOf(x, y, z, w))
    fun setInt4(name: String, value: Vector4i) = setInt4(name, value.x, value.y, value.z, value.w)

    fun setInts(name: String, values: IntArray)

    fun setFloat2(name: String, x: Float, y: Float) = setFloats(name, floatArrayOf(x, y))
    fun setFloat2(name: String, value: Vector2f) = setFloat2(name, value.x, value.y)

    fun setFloat3(name: String, x: Float, y: Float, z: Float) = setFloats(name, floatArrayOf(x, y, z))
    fun setFloat3(name: String, value: Vector3f) = setFloat3(name, value.x, value.y, value.z)

    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) = setFloats(name, floatArrayOf(x, y, z, w))
    fun setFloat4(name: String, value: Vector4f) = setFloat4(name, value.x, value.y, value.z, value.w)

    fun setColor(name: String, r: Float, g: Float, b: Float, a: Float = 1f) = setFloat4(name, r, g, b, a)
    fun setColor(name: String, value: Color) = setFloat4(name, value.r, value.g, value.b, value.a)

    fun setMat2(name: String, value: Matrix2f)
    fun setMat3(name: String, value: Matrix3f)
    fun setMat4(name: String, value: Matrix4f)

    fun setFloats(name: String, values: FloatArray)

    fun dispose()

    fun getId(): Int

}