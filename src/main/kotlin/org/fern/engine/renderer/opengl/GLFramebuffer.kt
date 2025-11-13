package org.fern.engine.renderer.opengl

import com.sun.jdi.IntegerType
import org.fern.engine.logger.logger
import org.fern.engine.renderer.Framebuffer
import org.fern.engine.renderer.FramebufferSpecs
import org.lwjgl.opengl.GL45C.*

class GLFramebuffer(override val specs: FramebufferSpecs) : Framebuffer {

    companion object {
        private val logger = logger()

        private var boundId = 0

        fun unbind() {
            if (boundId == 0) return

            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            boundId = 0
        }
    }

    private var id = 0
    private var colorAttachment = 0
    private var depthAttachment = 0

    override val colorAttachmentId get() = colorAttachment

    override val isBound get() = id == boundId

    init {
        if (invalidate())
            logger.info { "Created" }
    }

    private fun invalidate(): Boolean {
        if (id != 0)
            dispose()

        id = glCreateFramebuffers()

        colorAttachment = glCreateTextures(GL_TEXTURE_2D)
        glTextureStorage2D(colorAttachment, 1, GL_RGBA8, specs.width, specs.height)
        glTextureParameteri(colorAttachment, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTextureParameteri(colorAttachment, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTextureParameteri(colorAttachment, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTextureParameteri(colorAttachment, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glNamedFramebufferTexture(id, GL_COLOR_ATTACHMENT0, colorAttachment, 0)

        depthAttachment = glCreateRenderbuffers()
        glNamedRenderbufferStorage(depthAttachment, GL_DEPTH24_STENCIL8, specs.width, specs.height)
        glNamedFramebufferRenderbuffer(id, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthAttachment)

        glNamedFramebufferDrawBuffer(id, GL_COLOR_ATTACHMENT0)

        val status = glCheckNamedFramebufferStatus(id, GL_FRAMEBUFFER)
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            logger.error { "Incomplete (0x${Integer.toHexString(status)}" }
            return false
        }

        return true
    }

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            logger.error { "Attempted to resize with invalid dimensions (${width}x$height)" }
            return
        }

        specs.width = width
        specs.height = height
        invalidate()
        logger.trace { "Resized (${width}x$height)" }
    }

    override fun bind() {
        if (isBound) return

        glBindFramebuffer(GL_FRAMEBUFFER, id)
        boundId = id
    }

    override fun dispose() {
        if (id != 0) {
            glDeleteFramebuffers(id)
            id = 0
        }
        if (colorAttachment != 0) {
            glDeleteTextures(colorAttachment)
            colorAttachment = 0
        }
        if (depthAttachment != 0) {
            glDeleteRenderbuffers(depthAttachment)
            depthAttachment = 0
        }
    }

    override fun getId() = 0

}