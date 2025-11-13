package org.fern.engine.renderer.opengl

import org.fern.engine.renderer.BufferLayout
import org.fern.engine.renderer.IndexBuffer
import org.fern.engine.renderer.VertexBuffer
import org.fern.engine.logger.logger
import org.lwjgl.opengl.GL45C.*

class GLVertexBuffer(vertices: FloatArray) : VertexBuffer {

    companion object {
        private val logger = logger()

        private var boundId = 0

        fun unbind() {
            if (boundId == 0) return

            glBindBuffer(GL_ARRAY_BUFFER, 0)
            boundId = 0
        }
    }

    internal var id = glCreateBuffers()
        private set

    override var layout = BufferLayout()

    override val isBound get() = id == boundId

    init {
        glNamedBufferData(id, vertices, GL_STATIC_DRAW)
    }

    override fun bind() = glBindBuffer(GL_ARRAY_BUFFER, id)

    override fun dispose() {
        if (id == 0) return

        try {
            glDeleteBuffers(id)
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

class GLIndexBuffer(indices: IntArray) : IndexBuffer {

    companion object {
        private val logger = logger()

        private var boundId = 0

        fun unbind() {
            if (boundId == 0) return

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
            boundId = 0
        }
    }

    private var id = glCreateBuffers()

    override val count = indices.size

    override val isBound get() = id == boundId

    init {
        glNamedBufferData(id, indices, GL_STATIC_DRAW)

        logger.info { "Created" }
    }

    override fun bind() {
        if (isBound) return

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id)
        boundId = id
    }

    override fun dispose() {
        if (id == 0) return

        try {
            glDeleteBuffers(id)
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
