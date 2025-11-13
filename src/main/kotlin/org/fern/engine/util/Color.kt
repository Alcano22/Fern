package org.fern.engine.util

import org.fern.engine.logger.logger
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.math.roundToInt

data class Color(
    var r: Float,
    var g: Float,
    var b: Float,
    var a: Float = 1f
) {

    companion object {
        enum class HexFormat {
            AUTO,
            RGB,
            ARGB,
            RGBA
        }

        val TRANSPARENT get() = Color(0f, 0f, 0f, 0f)
        val WHITE       get() = Color(1f, 1f, 1f, 1f)
        val BLACK       get() = Color(0f, 0f, 0f, 1f)
        val RED         get() = Color(1f, 0f, 0f, 1f)
        val GREEN       get() = Color(0f, 1f, 0f, 1f)
        val BLUE        get() = Color(0f, 0f, 1f, 1f)

        private val logger = logger()

        private fun floatToByte(f: Float) = (f.coerceIn(0f, 1f) * 255f).roundToInt() and 0xFF
        private fun byteToFloat(i: Int) = (i and 0xFF) / 255f

        fun fromHex(hex: Int, format: HexFormat = HexFormat.AUTO): Color =
            when (format) {
                HexFormat.RGB -> {
                    val r = (hex ushr 16) and 0xFF
                    val g = (hex ushr 8) and 0xFF
                    val b = hex and 0xFF
                    Color(r, g, b, 255)
                }
                HexFormat.ARGB -> {
                    val a = (hex ushr 24) and 0xFF
                    val r = (hex ushr 16) and 0xFF
                    val g = (hex ushr 8) and 0xFF
                    val b = hex and 0xFF
                    Color(r, g, b, a)
                }
                HexFormat.RGBA -> {
                    val r = (hex ushr 24) and 0xFF
                    val g = (hex ushr 16) and 0xFF
                    val b = (hex ushr 8) and 0xFF
                    val a = hex and 0xFF
                    Color(r, g, b, a)
                }
                HexFormat.AUTO -> {
                    if ((hex ushr 24) != 0)
                        fromHex(hex, HexFormat.ARGB)
                    else
                        fromHex(hex, HexFormat.RGB)
                }
            }

        fun fromHexString(hexStr: String): Color {
            var str = hexStr.trim()
            if (str.startsWith('#')) {
                str = str.substring(1)
                when (str.length) {
                    6 -> {
                        val hex = str.toInt(16)
                        val r = (hex ushr 16) and 0xFF
                        val g = (hex ushr 8) and 0xFF
                        val b = hex and 0xFF
                        return Color(r, g, b, 255)
                    }
                    8 -> {
                        val hex = str.toLong(16).toInt()
                        val r = (hex ushr 24) and 0xFF
                        val g = (hex ushr 16) and 0xFF
                        val b = (hex ushr 8) and 0xFF
                        val a = hex and 0xFF
                        return Color(r, g, b, a)
                    }
                    else -> {
                        logger.error { "Unsupported hex color: $hexStr" }
                        return WHITE
                    }
                }
            } else if (str.startsWith("0x")) {
                str = str.substring(2)
                when (str.length) {
                    6 -> {
                        val hex = str.toInt(16)
                        val r = (hex ushr 16) and 0xFF
                        val g = (hex ushr 8) and 0xFF
                        val b = hex and 0xFF
                        return Color(r, g, b, 255)
                    }
                    8 -> {
                        val hex = str.toLong(16).toInt()
                        val a = (hex ushr 24) and 0xFF
                        val r = (hex ushr 16) and 0xFF
                        val g = (hex ushr 8) and 0xFF
                        val b = hex and 0xFF
                        return Color(r, g, b, a)
                    }
                    else -> {
                        logger.error { "Unsupported hex color: $hexStr" }
                        return WHITE
                    }
                }
            } else {
                logger.error { "Unsupported hex color: $hexStr" }
                return WHITE
            }
        }

        fun mix(a: Color, b: Color, t: Float): Color {
            val tt = t.coerceIn(0f, 1f)
            return Color(
                a.r + (b.r - a.r) * tt,
                a.g + (b.g - a.g) * tt,
                a.b + (b.b - a.b) * tt,
                a.a + (b.a - a.a) * tt
            )
        }
    }

    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        byteToFloat(r),
        byteToFloat(g),
        byteToFloat(b),
        byteToFloat(a)
    )

    constructor(v: Vector3i, alpha: Int = 255) : this(v.x, v.y, v.z, alpha)
    constructor(v: Vector4i) : this(v.x, v.y, v.z, v.w)

    constructor(v: Vector3f, alpha: Float = 1f) : this(v.x, v.y, v.z, alpha)
    constructor(v: Vector4f) : this(v.x, v.y, v.z, v.w)

    init { clamp() }

    fun clamp(): Color {
        r = r.coerceIn(0f, 1f)
        g = g.coerceIn(0f, 1f)
        b = b.coerceIn(0f, 1f)
        a = a.coerceIn(0f, 1f)
        return this
    }

    fun toVector3f() = Vector3f(r, g, b)
    fun toVector4f() = Vector4f(r, g, b, a)
    fun toVector3i() = Vector3i(
        floatToByte(r),
        floatToByte(g),
        floatToByte(b)
    )
    fun toVector4i() = Vector4i(
        floatToByte(r),
        floatToByte(g),
        floatToByte(b),
        floatToByte(a)
    )

    fun toFloatArray3() = floatArrayOf(r, g, b)
    fun toFloatArray4() = floatArrayOf(r, g, b, a)
    fun toIntArray3() = intArrayOf(
        floatToByte(r),
        floatToByte(g),
        floatToByte(b)
    )
    fun toIntArray4() = intArrayOf(
        floatToByte(r),
        floatToByte(g),
        floatToByte(b),
        floatToByte(a)
    )

    fun set(r: Float, g: Float, b: Float, a: Float = 1f): Color {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        return this
    }

    fun set(r: Int, g: Int, b: Int, a: Int = 255) = set(
        byteToFloat(r),
        byteToFloat(g),
        byteToFloat(b),
        byteToFloat(a)
    )

    fun set(other: Color) = set(other.r, other.g, other.b, other.a)

    fun set(v: Vector3f, alpha: Float = 1f) = set(v.x, v.y, v.z, alpha)
    fun set(v: Vector4f) = set(v.x, v.y, v.z, v.w)

    fun set(v: Vector3i, alpha: Int = 255) = set(v.x, v.y, v.z, alpha)
    fun set(v: Vector4i) = set(v.x, v.y, v.z, v.w)

    fun set(array: FloatArray) =
        when (val size = array.size) {
            3 -> set(array[0], array[1], array[2])
            4 -> set(array[0], array[1], array[2], array[3])
            else -> {
                logger.error { "Array has invalid size: $size" }
                array
            }
        }

    fun set(array: IntArray) =
        when (val size = array.size) {
            3 -> set(array[0], array[1], array[2])
            4 -> set(array[0], array[1], array[2], array[3])
            else -> {
                logger.error { "Array has invalid size: $size" }
                array
            }
        }

    fun get(v: Vector3f) = v.set(
        r.coerceIn(0f, 1f),
        g.coerceIn(0f, 1f),
        b.coerceIn(0f, 1f)
    )
    fun get(v: Vector4f) = v.set(
        r.coerceIn(0f, 1f),
        g.coerceIn(0f, 1f),
        b.coerceIn(0f, 1f),
        a.coerceIn(0f, 1f)
    )

    fun get(v: Vector3i) = v.set(
        floatToByte(r),
        floatToByte(g),
        floatToByte(b)
    )
    fun get(v: Vector4i) = v.set(
        floatToByte(r),
        floatToByte(g),
        floatToByte(b),
        floatToByte(a)
    )

    fun get(array: FloatArray): FloatArray {
        when (val size = array.size) {
            3 -> {
                array[0] = r
                array[1] = g
                array[2] = b
            }
            4 -> {
                array[0] = r
                array[1] = g
                array[2] = b
                array[3] = a
            }
            else -> logger.error { "Target array has invalid size: $size" }
        }
        return array
    }
    fun get(array: IntArray): IntArray {
        when (val size = array.size) {
            3 -> {
                array[0] = floatToByte(r)
                array[1] = floatToByte(g)
                array[2] = floatToByte(b)
            }
            4 -> {
                array[0] = floatToByte(r)
                array[1] = floatToByte(g)
                array[2] = floatToByte(b)
                array[3] = floatToByte(a)
            }
            else -> logger.error { "Target array has invalid size: $size" }
        }
        return array
    }

    fun toHexRGB(): Int {
        val ri = floatToByte(r)
        val gi = floatToByte(g)
        val bi = floatToByte(b)
        return (ri shl 16) or (gi shl 8) or bi
    }

    fun toHexARGB(): Int {
        val ai = floatToByte(a)
        return (ai shl 24) or toHexRGB()
    }

    fun toHexRGBA(): Int {
        val ri = floatToByte(r)
        val gi = floatToByte(g)
        val bi = floatToByte(b)
        val ai = floatToByte(a)
        return (ri shl 24) or (gi shl 16) or (bi shl 8) or ai
    }

    fun toHexStringRGB()  = String.format("#%06X", toHexRGB())
    fun toHexStringARGB() = String.format("#%08X", toHexARGB())
    fun toHexStringRGBA() = String.format("#%08X", toHexRGBA())

}
