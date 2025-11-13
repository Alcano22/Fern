package org.fern.engine.renderer.opengl

import org.fern.engine.renderer.IndexBuffer
import org.fern.engine.renderer.VertexArray
import org.fern.engine.renderer.VertexBuffer
import org.fern.engine.logger.logger
import org.lwjgl.opengl.GL45C.*

class GLVertexArray : VertexArray {

    companion object {
        private val logger = logger()

        private var boundId = 0

        fun unbind() {
            if (boundId == 0) return

            glBindVertexArray(0)
            boundId = 0
        }
    }

    private var id = glCreateVertexArrays()
    private var attribIndex = 0

    private val vertexBuffers = mutableListOf<VertexBuffer>()

    override var indexBuffer: IndexBuffer? = null
        set(value) {
            if (value != null) {
                glVertexArrayElementBuffer(id, value.getId())
            }
            field = value
        }

    override val hasIndexBuffer get() = indexBuffer != null

    override val isBound get() = id == boundId

    init {
        logger.info { "Created" }
    }

    override fun bind() {
        if (isBound) return

        glBindVertexArray(id)
        boundId = id
    }

    override fun addVertexBuffer(buffer: VertexBuffer) {
        require(buffer.layout.iterator().hasNext()) { "VertexBuffer has no layout!" }

        val layout = buffer.layout

        val bindingIndex = vertexBuffers.size
        glVertexArrayVertexBuffer(id, bindingIndex, buffer.getId(), 0L, layout.stride)

        var index = attribIndex
        for (element in layout) {
            if (element.isIntegerType) {
                glVertexArrayAttribIFormat(
                    id,
                    index,
                    element.componentCount,
                    element.glBaseType,
                    element.offset
                )
            } else {
                glVertexArrayAttribFormat(
                    id,
                    index,
                    element.componentCount,
                    element.glBaseType,
                    element.normalized,
                    element.offset
                )
            }

            glVertexArrayAttribBinding(id, index, bindingIndex)
            glEnableVertexArrayAttrib(id, index)

            index++
        }

        attribIndex = index
        vertexBuffers.add(buffer)
    }

    override fun dispose() {
        if (id == 0) return

        vertexBuffers.clear()
        indexBuffer = null

        try {
            glDeleteVertexArrays(id)
            logger.info { "Deleted" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete" }
        } finally {
            if (isBound)
                boundId = 0
            id = 0
        }
    }

    override fun getId() = id

}