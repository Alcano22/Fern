package org.fern.engine.renderer.opengl

import mu.KLogger
import org.fern.engine.logger.logger
import org.fern.engine.renderer.Texture
import org.fern.engine.renderer.TextureFilter
import org.fern.engine.renderer.TextureWrap
import org.fern.engine.resource.ResourceLoader
import org.fern.engine.util.memScoped
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL45C.*
import org.lwjgl.stb.STBImage.*
import java.nio.ByteBuffer
import kotlin.math.max

class GLTexture : Texture {

    private data class ImageData(
        val width: Int,
        val height: Int,
        val channels: Int
    )

    companion object {
        private var boundId = 0

        fun unbind() {
            if (boundId == 0) return

            glBindTextureUnit(0, 0)
            boundId = 0
        }

        private fun filterToGL(filter: TextureFilter) = when (filter) {
            TextureFilter.LINEAR                 -> GL_LINEAR
            TextureFilter.NEAREST                -> GL_NEAREST

            TextureFilter.NEAREST_MIPMAP_NEAREST -> GL_NEAREST_MIPMAP_NEAREST
            TextureFilter.NEAREST_MIPMAP_LINEAR  -> GL_NEAREST_MIPMAP_LINEAR
            TextureFilter.LINEAR_MIPMAP_NEAREST  -> GL_LINEAR_MIPMAP_NEAREST
            TextureFilter.LINEAR_MIPMAP_LINEAR   -> GL_LINEAR_MIPMAP_LINEAR
        }

        private fun wrapToGL(wrap: TextureWrap) = when (wrap) {
            TextureWrap.REPEAT          -> GL_REPEAT
            TextureWrap.CLAMP_TO_EDGE   -> GL_CLAMP_TO_EDGE
            TextureWrap.MIRRORED_REPEAT -> GL_MIRRORED_REPEAT
        }
    }

    override val filepath: String
    override val width: Int
    override val height: Int

    private var id = 0

    override val isBound get() = id == boundId

    override var minFilter = TextureFilter.LINEAR
        set(value) {
            field = value
            if (id == 0) {
                logger.error { "Failed to set minFilter because texture is invalid" }
                return
            }

            val glValue = filterToGL(value)
            glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, glValue)
        }

    override var magFilter = TextureFilter.LINEAR
        set(value) {
            field = value
            if (id == 0) {
                logger.error { "Failed to set magFilter because texture is invalid" }
                return
            }

            val glValue = filterToGL(value)
            glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, glValue)
        }

    override var wrapS = TextureWrap.REPEAT
        set(value) {
            field = value
            if (id == 0) {
                logger.error { "Failed to set wrapS because texture is invalid" }
                return
            }

            val glValue = wrapToGL(value)
            glTextureParameteri(id, GL_TEXTURE_WRAP_S, glValue)
        }

    override var wrapT = TextureWrap.REPEAT
        set(value) {
            field = value
            if (id == 0) {
                logger.error { "Failed to set wrapT because texture is invalid" }
                return
            }

            val glValue = wrapToGL(value)
            glTextureParameteri(id, GL_TEXTURE_WRAP_T, glValue)
        }

    private val logger: KLogger

    constructor(filepath: String, generateMipmaps: Boolean = true) {
        this.filepath = filepath
        logger = logger(filepath)

        val bytes = try {
            ResourceLoader.loadBytes(filepath)
        } catch (e: Exception) {
            logger.error(e) { "Failed to read bytes" }

            val (texId, w, h) = createFallbackWhiteTexture()
            id = texId
            width = w
            height = h
            return
        }

        val imgBuf = BufferUtils.createByteBuffer(bytes.size).apply {
            put(bytes)
            flip()
        }

        val (imgData, pixels) = memScoped {
            val pw = mallocInt(1)
            val ph = mallocInt(1)
            val pc = mallocInt(1)

            stbi_set_flip_vertically_on_load(true)
            val img = stbi_load_from_memory(imgBuf, pw, ph, pc, 0)
            stbi_set_flip_vertically_on_load(false)

            if (img != null) {
                val meta = ImageData(pw[0], ph[0], pc[0])
                meta to img
            } else
                Pair<ImageData?, ByteBuffer?>(null, null)
        }

        if (imgData == null || pixels == null) {
            logger.error { "Failed to decode" }
            val (texId, w, h) = createFallbackWhiteTexture()
            id = texId
            width = w
            height = h
            return
        }

        val width = imgData.width
        val height = imgData.height
        val channels = imgData.channels

        val (internalFormat, dataFormat) = when (channels) {
            3 -> GL_RGB8 to GL_RGB
            4 -> GL_RGBA8 to GL_RGBA
            else -> {
                logger.error { "Unsupported channel count: $channels" }
                val (texId, w, h) = createFallbackWhiteTexture()
                id = texId
                this.width = w
                this.height = h
                stbi_image_free(pixels)
                return
            }
        }

        this.width = width
        this.height = height

        id = glCreateTextures(GL_TEXTURE_2D)

        glTextureStorage2D(id, 1, internalFormat, width, height)

        val prevUnpack = IntArray(1)
        glGetIntegerv(GL_UNPACK_ALIGNMENT, prevUnpack)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        glTextureSubImage2D(
            id,
            0,
            0, 0,
            width, height,
            dataFormat,
            GL_UNSIGNED_BYTE,
            pixels
        )

        if (generateMipmaps)
            glGenerateTextureMipmap(id)

        glPixelStorei(GL_UNPACK_ALIGNMENT, prevUnpack[0])

        stbi_image_free(pixels)

        logger.info { "Loaded (${width}x$height)" }
    }

    constructor(width: Int, height: Int, name: String) {
        this.filepath = name
        logger = logger(name)

        this.width = max(1, width)
        this.height = max(1, height)

        id = glCreateTextures(GL_TEXTURE_2D)
        glTextureStorage2D(id, 1, GL_RGBA8, this.width, this.height)

        logger.info { "Created empty texture (${this.width}x${this.height})" }
    }

    private fun createFallbackWhiteTexture(): Triple<Int, Int, Int> {
        val tex = glCreateTextures(GL_TEXTURE_2D)
        glTextureStorage2D(tex, 1, GL_RGBA8, 1, 1)

        glTextureParameteri(tex, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTextureParameteri(tex, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTextureParameteri(tex, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(tex, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        val pixels = BufferUtils.createByteBuffer(4).apply {
            repeat(4) { put(255.toByte()) }
            flip()
        }

        val prevUnpack = IntArray(1)
        glGetIntegerv(GL_UNPACK_ALIGNMENT, prevUnpack)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTextureSubImage2D(tex, 0, 0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
        glPixelStorei(GL_UNPACK_ALIGNMENT, prevUnpack[0])

        logger.warn { "Created 1x1 fallback" }
        return Triple(tex, 1, 1)
    }

    override fun generateMipmaps() = glGenerateTextureMipmap(id)

    override fun bind() {
        if (isBound) return

        glBindTextureUnit(0, id)
        boundId = id
    }

    override fun dispose() {
        if (id == 0) return

        try {
            glDeleteTextures(id)
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