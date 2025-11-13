package org.fern.engine.renderer

import org.fern.engine.renderer.opengl.GLIndexBuffer
import org.fern.engine.renderer.opengl.GLVertexBuffer
import org.lwjgl.opengl.GL45C.*

enum class ShaderDataType(
    val componentSize: Int,
    val componentCount: Int,
    val glBaseType: Int
) {

    BOOL(1, 1, GL_BOOL),
    INT(Int.SIZE_BYTES, 1, GL_INT),
    INT2(Int.SIZE_BYTES, 2, GL_INT),
    INT3(Int.SIZE_BYTES, 3, GL_INT),
    INT4(Int.SIZE_BYTES, 4, GL_INT),
    FLOAT(Float.SIZE_BYTES, 1, GL_FLOAT),
    FLOAT2(Float.SIZE_BYTES, 2, GL_FLOAT),
    FLOAT3(Float.SIZE_BYTES, 3, GL_FLOAT),
    FLOAT4(Float.SIZE_BYTES, 4, GL_FLOAT),
    MAT2(Float.SIZE_BYTES, 2 * 2, GL_FLOAT),
    MAT3(Float.SIZE_BYTES, 3 * 3, GL_FLOAT),
    MAT4(Float.SIZE_BYTES, 4 * 4, GL_FLOAT);

    val size = componentSize * componentCount
    val isIntegerType = glBaseType == GL_BOOL || glBaseType == GL_INT
}

data class BufferElement(
    val type: ShaderDataType,
    val name: String,
    val normalized: Boolean
) {
    val size = type.size
    val componentCount = type.componentCount
    val glBaseType = type.glBaseType
    val isIntegerType = type.isIntegerType

    var offset = 0
}

class BufferLayout internal constructor(
    val elements: List<BufferElement> = emptyList()
) : Iterable<BufferElement> {

    var stride = 0
        private set

    init {
        calculateOffsetsAndStride()
    }

    private fun calculateOffsetsAndStride() {
        var offset = 0
        stride = 0
        for (element in elements) {
            element.offset = offset
            offset += element.size
            stride += element.size
        }
    }

    override fun iterator() = elements.iterator()

    operator fun get(index: Int) = elements[index]

    fun findByName(name: String) = elements.find { it.name == name }
}

class BufferLayoutBuilder {
    private val elements = mutableListOf<BufferElement>()

    fun int(name: String) = add(ShaderDataType.INT, name, false)
    fun int2(name: String) = add(ShaderDataType.INT2, name, false)
    fun int3(name: String) = add(ShaderDataType.INT3, name, false)
    fun int4(name: String) = add(ShaderDataType.INT4, name, false)

    fun float(name: String, normalized: Boolean = false) = add(ShaderDataType.FLOAT, name, normalized)
    fun float2(name: String, normalized: Boolean = false) = add(ShaderDataType.FLOAT2, name, normalized)
    fun float3(name: String, normalized: Boolean = false) = add(ShaderDataType.FLOAT3, name, normalized)
    fun float4(name: String, normalized: Boolean = false) = add(ShaderDataType.FLOAT4, name, normalized)

    fun mat2(name: String) = add(ShaderDataType.MAT2, name, false)
    fun mat3(name: String) = add(ShaderDataType.MAT3, name, false)
    fun mat4(name: String) = add(ShaderDataType.MAT4, name, false)

    private fun add(type: ShaderDataType, name: String, normalized: Boolean) {
        elements.add(BufferElement(type, name, normalized))
    }

    internal fun build() = BufferLayout(elements.toList())
}

fun bufferLayout(block: BufferLayoutBuilder.() -> Unit): BufferLayout {
    val b = BufferLayoutBuilder()
    b.block()
    return b.build()
}

interface Buffer {
    val isBound: Boolean

    fun bind()
    fun dispose()

    fun getId(): Int
}

interface VertexBuffer : Buffer {
    companion object {
        fun create(vertices: FloatArray) = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLVertexBuffer(vertices)
        }

        fun unbind() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLVertexBuffer.unbind()
        }
    }

    var layout: BufferLayout
}

interface IndexBuffer : Buffer {
    companion object {
        fun create(indices: IntArray) = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLIndexBuffer(indices)
        }

        fun unbind() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLIndexBuffer.unbind()
        }
    }

    val count: Int
}
