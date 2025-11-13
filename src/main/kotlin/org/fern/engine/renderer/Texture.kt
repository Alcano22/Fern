package org.fern.engine.renderer

import org.fern.engine.renderer.opengl.GLTexture
import org.fern.engine.resource.AssetMetaService

enum class TextureFilter {
    LINEAR,
    NEAREST,

    NEAREST_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_NEAREST,
    LINEAR_MIPMAP_LINEAR
}

enum class TextureWrap {
    REPEAT,
    CLAMP_TO_EDGE,
    MIRRORED_REPEAT
}

object TextureFactory {
    fun createRaw(filepath: String, generateMipmaps: Boolean = true): Texture = when (Renderer.apiType) {
        RenderAPI.Type.OPEN_GL -> GLTexture(filepath, generateMipmaps)
    }

    fun createRaw(width: Int, height: Int, name: String): Texture = when (Renderer.apiType) {
        RenderAPI.Type.OPEN_GL -> GLTexture(width, height, name)
    }
}

interface Texture {

    companion object {
        fun create(filepath: String, generateMipmaps: Boolean = true): Texture =
            if (filepath.startsWith("user:", ignoreCase = true))
                TextureLibrary.get(filepath)
            else
                TextureFactory.createRaw(filepath, generateMipmaps)

        fun create(width: Int, height: Int, name: String) = TextureFactory.createRaw(width, height, name)

        fun unbind() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLTexture.unbind()
        }
    }

    val filepath: String

    val width: Int
    val height: Int

    val isBound: Boolean

    var minFilter: TextureFilter
    var magFilter: TextureFilter

    var wrapS: TextureWrap
    var wrapT: TextureWrap

    fun generateMipmaps()

    fun bind()

    fun dispose()

    fun setParams(
        minFilter: TextureFilter = TextureFilter.LINEAR,
        magFilter: TextureFilter = TextureFilter.LINEAR,
        wrapS: TextureWrap = TextureWrap.REPEAT,
        wrapT: TextureWrap = TextureWrap.REPEAT
    ) {
        this.minFilter = minFilter
        this.magFilter = magFilter
        this.wrapS = wrapS
        this.wrapT = wrapT
    }

    fun getId(): Int

}