package org.fern.engine.resource

import org.fern.engine.logger.logger
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object ResourceLoader {

    private data class ResourceKey(val domain: ResourceDomain, val path: String)

    private val ASSETS_ROOT = Paths.get("assets")

    private val logger = logger()

    @Volatile
    private var cache: MutableMap<ResourceKey, Resource> = ConcurrentHashMap()

    @Synchronized
    fun configure(maxCacheEntries: Int? = null) {
        cache.values.forEach { it.close() }
        cache.clear()

        cache = if (maxCacheEntries == null)
            ConcurrentHashMap()
        else {
            val map = object : LinkedHashMap<ResourceKey, Resource>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<ResourceKey, Resource>?) = size > maxCacheEntries
            }
            Collections.synchronizedMap(map)
        }
    }

    fun loadResource(domain: ResourceDomain, path: String, shouldCache: Boolean = true): Resource {
        val key = ResourceKey(domain, path)

        if (!shouldCache) {
            val bytes = loadBytesUncached(domain, path)
            logger.info { "Loaded ${domain.name.lowercase()} resource: $path" }
            return Resource(domain, path, bytes)
        }

        val map = cache
        if (map is ConcurrentHashMap<*, *>) {
            val concurrent = map as ConcurrentHashMap<ResourceKey, Resource>
            return concurrent.computeIfAbsent(key) { key ->
                val bytes = loadBytesUncached(key.domain, key.path)
                logger.info { "Loaded ${domain.name.lowercase()} resources: ${key.path}" }
                Resource(key.domain, key.path, bytes)
            }
        }

        synchronized(map) {
            return map[key] ?: run {
                val bytes = loadBytesUncached(domain, path)
                logger.info { "Loaded ${domain.name.lowercase()} resource: $path" }
                val res = Resource(domain, path, bytes)
                map[key] = res
                res
            }
        }
    }

    fun loadBytes(domain: ResourceDomain, path: String, shouldCache: Boolean = true): ByteArray {
        val res = loadResource(domain, path, shouldCache)
        if (res.isClosed) {
            invalidate(domain, path)
            return loadBytes(domain, path, shouldCache)
        }
        return res.bytes
    }

    fun loadText(
        domain: ResourceDomain,
        path: String,
        charset: Charset = Charsets.UTF_8,
        shouldCache: Boolean = true
    ): String {
        val bytes = loadBytes(domain, path, shouldCache)
        return String(bytes, charset)
    }

    fun exists(domain: ResourceDomain, path: String) =
        getResourceStream(domain, path)?.use { true } ?: false

    fun preload(domain: ResourceDomain, path: String) {
        loadBytes(domain, path, true)
    }

    fun invalidate(domain: ResourceDomain, path: String) {
        val removed = cache.remove(ResourceKey(domain, path))
        removed?.close()
    }

    fun loadResource(path: String, shouldCache: Boolean = true): Resource {
        val parsed = parsePath(path)
        return when (parsed) {
            is ParsedPath.Explicit -> loadResource(parsed.domain, parsed.cleanPath, shouldCache)
            is ParsedPath.Implicit -> {
                try {
                    loadResource(ResourceDomain.INTERNAL, parsed.path, shouldCache)
                } catch (_: ResourceNotFoundException) {
                    loadResource(ResourceDomain.USER, parsed.path, shouldCache)
                }
            }
        }
    }

    fun loadBytes(path: String, shouldCache: Boolean = true): ByteArray {
        val res = loadResource(path, shouldCache)
        if (res.isClosed) {
            invalidate(res.domain, res.path)
            return loadBytes(path, shouldCache)
        }
        return res.bytes
    }

    fun loadText(path: String, charset: Charset = Charsets.UTF_8, shouldCache: Boolean = true): String {
        val bytes = loadBytes(path, shouldCache)
        return String(bytes, charset)
    }

    fun exists(path: String) = when (val parsed = parsePath(path)) {
        is ParsedPath.Explicit -> exists(parsed.domain, parsed.cleanPath)
        is ParsedPath.Implicit ->
            exists(ResourceDomain.INTERNAL, parsed.path) ||
            exists(ResourceDomain.USER, parsed.path)
    }

    fun preload(path: String) {
        loadBytes(path, true)
    }

    fun invalidate(path: String) {
        when (val parsed = parsePath(path)) {
            is ParsedPath.Explicit -> invalidate(parsed.domain, parsed.cleanPath)
            is ParsedPath.Implicit -> {
                invalidate(ResourceDomain.INTERNAL, parsed.path)
                invalidate(ResourceDomain.USER, parsed.path)
            }
        }
    }

    fun clearCache() {
        cache.values.forEach { it.close() }
        cache.clear()
    }

    private sealed interface ParsedPath {
        data class Explicit(val domain: ResourceDomain, val cleanPath: String) : ParsedPath
        data class Implicit(val path: String) : ParsedPath
    }

    private fun parsePath(path: String): ParsedPath {
        val p = path.trim()
        val lower = p.lowercase()

        return when {
            lower.startsWith("internal:") -> ParsedPath.Explicit(ResourceDomain.INTERNAL, p.substringAfter(':'))
            lower.startsWith("user:")     -> ParsedPath.Explicit(ResourceDomain.USER, p.substringAfter(':'))
            else -> ParsedPath.Implicit(p)
        }
    }

    private fun loadBytesUncached(domain: ResourceDomain, path: String): ByteArray {
        val stream = getResourceStream(domain, path) ?: throw ResourceNotFoundException(domain, path)
        return stream.use { it.readAllBytes() }
    }

    private fun getResourceStream(domain: ResourceDomain, path: String): InputStream? =
        when (domain) {
            ResourceDomain.INTERNAL -> getInternalResourceStream(path)
            ResourceDomain.USER     -> getUserResourceStream(path)
        }

    private fun getInternalResourceStream(path: String): InputStream? {
        val normalized = path.trimStart('/')

        Thread.currentThread().contextClassLoader
            ?.getResourceAsStream(normalized)
            ?.let { return it }

        ResourceLoader::class.java
            .getResourceAsStream("/$normalized")
            ?.let { return it }

        return null
    }

    private fun getUserResourceStream(path: String): InputStream? {
        val base = ASSETS_ROOT.toAbsolutePath().normalize()
        val resolved = base.resolve(path).normalize()

        if (!resolved.startsWith(base)) {
            logger.warn { "Blocked path traversel outside assets root: $resolved" }
            return null
        }

        return try {
            if (Files.exists(resolved) && Files.isRegularFile(resolved))
                Files.newInputStream(resolved)
            else
                null
        } catch (_: Exception) { null }
    }

}

class ResourceNotFoundException : RuntimeException {
    val domain: ResourceDomain?
    val path: String

    constructor(path: String) : super("Resource not found: $path") {
        this.domain = null
        this.path = path
    }

    constructor(domain: ResourceDomain, path: String) : super("Resource not found ($domain): $path") {
        this.domain = domain
        this.path = path
    }
}
