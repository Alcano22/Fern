package org.fern.engine.renderer.opengl

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import mu.KLogger
import org.fern.engine.renderer.Shader
import org.fern.engine.resource.ResourceLoader
import org.fern.engine.logger.logger
import org.fern.engine.util.memScoped
import org.fern.engine.util.mutableHashMapOf
import org.joml.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL45C.*
import java.util.HashSet
import kotlin.math.abs

class GLShader : Shader {

    companion object {
        private var boundId = 0

        fun unbind() {
            if (boundId == 0) return

            glUseProgram(0)
            boundId = 0
        }

        private fun shaderTypeFromString(type: String): Int =
            when (type.trim()) {
                "vertex"   -> GL_VERTEX_SHADER
                "fragment" -> GL_FRAGMENT_SHADER
                else -> error("Unknown shader type: $type")
            }

        private fun readFile(filepath: String) =
            ResourceLoader.loadText(filepath, shouldCache = true)

        private fun getConstants() = buildString {
            appendLine("#define PI 3.14159265359")
            appendLine("#define E 2.71828182846")
        }

        private fun floatsEqual(a: FloatArray, b: FloatArray, eps: Float = 1e-6f): Boolean {
            if (a.size != b.size)
                return false
            for (i in a.indices) {
                if (abs(a[i] - b[i]) > eps)
                    return false
            }
            return true
        }
    }

    private val sources = mutableHashMapOf<Int, String>()

    var name = ""
        private set

    private var id = 0

    private val uniformLocationCache = Object2IntOpenHashMap<String>().apply { defaultReturnValue(-1) }
    private val warnedUniforms: MutableSet<String> = HashSet()

    private val cacheInt = Int2IntOpenHashMap()
    private val cacheFloat = Int2FloatOpenHashMap()

    private val cacheInts = Int2ObjectOpenHashMap<IntArray>()
    private val cacheFloats = Int2ObjectOpenHashMap<FloatArray>()

    private val logger: KLogger

    override val isBound get() = id == boundId

    init {
        val caps = GL.getCapabilities()
        val supportsProgramUniforms = caps.glProgramUniform1f != 0L
        if (!supportsProgramUniforms)
            throw IllegalStateException("glProgramUniform not available. Shader requires OpenGL 4.1+")
    }

    constructor(name: String, vertexSrc: String, fragmentSrc: String) {
        this.name = name
        logger = logger(name)
        sources[GL_VERTEX_SHADER] = vertexSrc
        sources[GL_FRAGMENT_SHADER] = fragmentSrc
    }

    constructor(filepath: String) {
        val src = readFile(filepath)
        preprocess(src)
        val lastSlash = filepath.lastIndexOfAny(charArrayOf('/', '\\')).let {
            if (it == -1) 0 else it + 1
        }
        val lastDot = filepath.lastIndexOf('.')
        val count = if (lastDot == -1)
            filepath.length - lastSlash
        else
            lastDot - lastSlash
        name = filepath.substring(lastSlash, lastSlash + count)
        logger = logger(name)
    }

    private fun preprocess(src: String) {
        val token = "#type"
        var pos = src.indexOf(token)
        while (pos != -1) {
            val eol = src.indexOfFirst { it == '\r' || it == '\n' }.let {
                val sub = src.substring(pos)
                val idx = sub.indexOfFirst { ch -> ch == '\r' || ch == '\n' }
                if (idx == -1) -1 else pos + idx
            }
            require(eol != -1) { "Shader syntax error: Missing end of line after #type" }

            var begin = pos + token.length
            while (begin < src.length && src[begin].isWhitespace())
                begin++

            var typeStr = src.substring(begin, eol)
            val lastNonWs = typeStr.indexOfLast { !it.isWhitespace() }
            typeStr = if (lastNonWs != -1) typeStr.substring(0, lastNonWs + 1) else ""

            val type = shaderTypeFromString(typeStr)
            require(type != 0) { "Invalid shader type specifier" }

            val nextLinePos = src.indexOfFirst { ch -> ch != '\r' && ch != '\n' }.let {
                val sub = src.substring(eol + 1)
                val idx = sub.indexOfFirst { ch -> ch != '\r' && ch != '\n' }
                if (idx == -1) -1 else eol + 1 + idx
            }
            require(nextLinePos != -1) { "Shader syntax error: Missing shader source after type declaration" }

            val nextToken = src.indexOf(token, nextLinePos)
            val len = if (nextToken == -1) src.length - nextLinePos else nextToken - nextLinePos
            sources[type] = src.substring(nextLinePos, nextLinePos + len)

            pos = nextToken
        }
    }

    override fun compile() {
        uniformLocationCache.clear()
        warnedUniforms.clear()

        cacheInt.clear()
        cacheFloat.clear()

        cacheInts.clear()
        cacheFloats.clear()

        if (id != 0) {
            glDeleteProgram(id)
            id = 0
            logger.trace { "Deleted previous shader program (id reset)" }
        }

        require(sources.size <= 2) { "Fern only supports 2 shader stages" }

        val constants = getConstants()
        val compiledShaders = ArrayList<Int>(sources.size)
        val program = glCreateProgram()

        for ((type, src) in sources) {
            val srcWithConstants = run {
                val versionPos = src.indexOf("#version")
                if (versionPos != -1) {
                    val versionEnd = src.indexOfFirst { it == '\r' || it == '\n' }.let {
                        val sub = src.substring(versionPos)
                        val idx = sub.indexOfFirst { ch -> ch == '\r' || ch == '\n' }
                        if (idx == -1) -1 else versionPos + idx
                    }
                    if (versionEnd != -1) {
                        val versionLine = src.substring(versionPos, versionEnd + 1)
                        versionLine + '\n' + constants + '\n' + src.substring(versionEnd + 1)
                    } else
                        constants + '\n' + src
                } else
                    constants + '\n' + src
            }

            val shader = glCreateShader(type)
            glShaderSource(shader, srcWithConstants)
            glCompileShader(shader)

            val isCompiled = glGetShaderi(shader, GL_COMPILE_STATUS)
            if (isCompiled == GL_FALSE) {
                val msg = glGetShaderInfoLog(shader)
                compiledShaders.forEach { glDeleteShader(it) }
                logger.error { "Failed to compile stage '$type':\n$msg" }
                glDeleteProgram(program)
                return
            }

            glAttachShader(program, shader)
            compiledShaders.add(shader)
            logger.trace { "Compiled stage $type" }
        }

        glLinkProgram(program)
        val isLinked = glGetProgrami(program, GL_LINK_STATUS)
        if (isLinked == GL_FALSE) {
            val msg = glGetProgramInfoLog(program)
            compiledShaders.forEach {
                glDetachShader(program, it)
                glDeleteShader(it)
            }
            glDeleteProgram(program)
            logger.error { "Failed to link:\n$msg" }
            return
        }

        compiledShaders.forEach {
            glDetachShader(program, it)
            glDeleteShader(it)
        }

        id = program
        logger.trace { "Linked" }
        logger.info { "Compiled" }
    }

    override fun bind() {
        if (isBound) return

        if (id != 0) {
            glUseProgram(id)
            boundId = id
        } else
            logger.warn { "Attempted to bind but program id is 0" }
    }

    private fun getUniformLocation(name: String): Int {
        if (id == 0) {
            logger.error { "Called getUniformLocation on invalid shader" }
            return -1
        }

        if (uniformLocationCache.containsKey(name))
            return uniformLocationCache.getInt(name)

        val loc = glGetUniformLocation(id, name)
        uniformLocationCache.put(name, loc)
        if (loc == -1 && warnedUniforms.add(name))
            logger.warn { "Uniform '$name' not found" }
        return loc
    }

    override fun setInt(name: String, value: Int) {
        val loc = getUniformLocation(name)
        if (loc == -1) return

        if (cacheInt.containsKey(loc)) {
            val cached = cacheInt.get(loc)
            if (cached == value) return
        }

        cacheInt.put(loc, value)
        try {
            glProgramUniform1i(id, loc, value)
            logger.trace { "Set int uniform '$name'" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set int uniform '$name'" }
        }
    }

    override fun setFloat(name: String, value: Float) {
        val loc = getUniformLocation(name)
        if (loc == -1) return

        if (cacheFloat.containsKey(loc)) {
            val cached = cacheFloat.get(loc)
            if (cached == value) return
        }

        cacheFloat.put(loc, value)
        try {
            glProgramUniform1f(id, loc, value)
            logger.trace { "Set float uniform '$name'" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set float uniform '$name'" }
        }
    }

    override fun setInts(name: String, values: IntArray) {
        val loc = getUniformLocation(name)
        if (loc == -1) return

        val cached = cacheInts.get(loc)
        if (cached != null && cached.contentEquals(values)) return

        cacheInts.put(loc, values.copyOf())
        try {
            when (values.size) {
                2 -> glProgramUniform2iv(id, loc, values)
                3 -> glProgramUniform3iv(id, loc, values)
                4 -> glProgramUniform4iv(id, loc, values)
                else -> throw IllegalArgumentException("Unsupported values size: ${values.size}")
            }
            logger.trace { "Set ints uniform '$name'" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set ints uniform '$name'" }
        }
    }

    override fun setMat2(name: String, value: Matrix2f) {
        val loc = getUniformLocation(name)
        if (loc == -1) return

        val arr = FloatArray(2 * 2)
        value.get(arr)

        val cached = cacheFloats.get(loc)
        if (cached != null && floatsEqual(cached, arr)) return

        cacheFloats.put(loc, arr.copyOf())
        memScoped {
            val fb = mallocFloat(2 * 2)
            fb.put(arr).flip()
            try {
                glProgramUniformMatrix2fv(id, loc, false, fb)
                logger.trace { "Set mat2 uniform '$name'" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to set mat2 uniform '$name'" }
            }
        }
    }

    override fun setMat3(name: String, value: Matrix3f) {
        val loc = getUniformLocation(name)
        if (loc == -1) return

        val arr = FloatArray(3 * 3)
        value.get(arr)

        val cached = cacheFloats.get(loc)
        if (cached != null && floatsEqual(cached, arr)) return

        cacheFloats.put(loc, arr.copyOf())
        memScoped {
            val fb = mallocFloat(3 * 3)
            fb.put(arr).flip()
            try {
                glProgramUniformMatrix3fv(id, loc, false, fb)
                logger.trace { "Set mat3 uniform '$name'" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to set mat3 uniform '$name'" }
            }
        }
    }

    override fun setMat4(name: String, value: Matrix4f) {
        val loc = getUniformLocation(name)
        if (loc == -1) return

        val arr = FloatArray(4 * 4)
        value.get(arr)

        val cached = cacheFloats.get(loc)
        if (cached != null && floatsEqual(cached, arr)) return

        cacheFloats.put(loc, arr.copyOf())
        memScoped {
            val fb = mallocFloat(4 * 4)
            fb.put(arr).flip()
            try {
                glProgramUniformMatrix4fv(id, loc, false, fb)
                logger.trace { "Set mat4 uniform '$name'" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to set mat4 uniform '$name'" }
            }
        }
    }

    override fun setFloats(name: String, values: FloatArray) {
        val loc = getUniformLocation(name)
        if (loc == -1) return

        val cached = cacheFloats.get(loc)
        if (cached != null && floatsEqual(cached, values)) return

        cacheFloats.put(loc, values.copyOf())
        try {
            when (values.size) {
                2 -> glProgramUniform2fv(id, loc, values)
                3 -> glProgramUniform3fv(id, loc, values)
                4 -> glProgramUniform4fv(id, loc, values)
                else -> throw IllegalArgumentException("Unsupported values size: ${values.size}")
            }
            logger.trace { "Set floats uniform '$name'" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set floats uniform '$name'" }
        }
    }

    override fun dispose() {
        if (id == 0) return

        try {
            glDeleteProgram(id)
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