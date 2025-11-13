package org.fern.engine.resource

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.fern.engine.renderer.Texture
import org.fern.engine.renderer.TextureFilter
import org.fern.engine.renderer.TextureWrap
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class TextureMeta(
    var minFilter: TextureFilter = TextureFilter.LINEAR,
    var magFilter: TextureFilter = TextureFilter.LINEAR,
    var wrapS: TextureWrap = TextureWrap.REPEAT,
    var wrapT: TextureWrap = TextureWrap.REPEAT,
    var generateMipmaps: Boolean = true
)

object AssetMetaService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun metaPathForTexture(filepath: String): Path? {
        if (filepath.startsWith("internal:")) return null

        val normalizedSlash = filepath.replace('\\', '/')
        val isAbsWindows = Regex("^[A-Za-z]:/").containsMatchIn(normalizedSlash)
        val isAbsPosix = normalizedSlash.startsWith('/')

        if (isAbsWindows || isAbsPosix) {
            val abs = Paths.get(filepath)
            return abs.parent?.resolve(abs.fileName.toString() + ".meta")
        }

        val schemeMatch = Regex("^([A-Za-z]+):").find(normalizedSlash)
        var rel = normalizedSlash
        if (schemeMatch != null) {
            val scheme = schemeMatch.groupValues[1].lowercase()
            when (scheme) {
                "user" -> rel = normalizedSlash.substringAfter(':')
                "internal" -> return null
                else -> return null
            }
        }

        if (rel.startsWith("assets/"))
            rel = rel.removePrefix("assets/")

        val abs = Paths.get("assets").resolve(rel).normalize()
        return abs.parent?.resolve(abs.fileName.toString() + ".meta")
    }

    fun loadTextureMeta(filepath: String): TextureMeta? {
        val p = metaPathForTexture(filepath) ?: return null
        if (!p.exists() || !p.isRegularFile()) return null

        return runCatching {
            json.decodeFromString(TextureMeta.serializer(), p.readText())
        }.getOrNull()
    }

    fun saveTextureMeta(filepath: String, meta: TextureMeta) {
        val p = metaPathForTexture(filepath) ?: return
        runCatching { Files.createDirectories(p.parent) }
        runCatching {
            p.writeText(json.encodeToString(TextureMeta.serializer(), meta))
        }
    }

    fun saveFromTexture(tex: Texture, generateMipmaps: Boolean? = null) {
        val meta = TextureMeta(
            minFilter = tex.minFilter,
            magFilter = tex.magFilter,
            wrapS = tex.wrapS,
            wrapT = tex.wrapT,
            generateMipmaps = generateMipmaps ?: true
        )
        saveTextureMeta(tex.filepath, meta)
    }

    fun applyToTexture(tex: Texture, meta: TextureMeta) {
        tex.minFilter = meta.minFilter
        tex.magFilter = meta.magFilter
        tex.wrapS = meta.wrapS
        tex.wrapT = meta.wrapT
    }

    fun readGenerateMipmapsFlag(filepath: String) =
        loadTextureMeta(filepath)?.generateMipmaps ?: true

}