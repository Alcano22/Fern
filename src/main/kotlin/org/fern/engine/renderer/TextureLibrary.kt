package org.fern.engine.renderer

import org.fern.engine.resource.AssetMetaService
import java.util.concurrent.ConcurrentHashMap

object TextureLibrary {

    private val cache = ConcurrentHashMap<String, Texture>()

    private fun canon(filepath: String): String {
        require(filepath.startsWith("user:", ignoreCase = true)) {
            "TextureLibrary expects 'user:' paths. Got $filepath"
        }
        val norm = filepath.replace('\\', '/')
        val rel = norm.substringAfter(':').trimStart('/')
        return "user:$rel"
    }

    fun get(filepath: String, reload: Boolean = false): Texture {
        val key = canon(filepath)
        if (reload)
            invalidate(key)
        return cache.getOrPut(key) {
            val genMips = AssetMetaService.readGenerateMipmapsFlag(key)
            val tex = TextureFactory.createRaw(key, genMips)
            AssetMetaService.loadTextureMeta(key)?.let { AssetMetaService.applyToTexture(tex, it) }
            tex
        }
    }

    fun applyMetaFor(filepath: String) {
        val key = canon(filepath)
        val tex = cache[key] ?: return
        val meta = AssetMetaService.loadTextureMeta(key) ?: return

        AssetMetaService.applyToTexture(tex, meta)

        val usesMip = when (meta.minFilter) {
            TextureFilter.NEAREST_MIPMAP_NEAREST,
            TextureFilter.NEAREST_MIPMAP_LINEAR,
            TextureFilter.LINEAR_MIPMAP_NEAREST,
            TextureFilter.LINEAR_MIPMAP_LINEAR -> true
            else -> false
        }
        if (usesMip)
            runCatching { tex.generateMipmaps() }
    }

    fun invalidate(filepath: String) {
        val key = canon(filepath)
        cache.remove(key)?.dispose()
    }

    fun clear() {
        cache.values.forEach { runCatching { it.dispose() } }
        cache.clear()
    }

}