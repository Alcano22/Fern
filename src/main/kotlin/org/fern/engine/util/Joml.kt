package org.fern.engine.util

import org.joml.*

fun Vector2i.toArray() = intArrayOf(x, y)
fun Vector3i.toArray() = intArrayOf(x, y, z)
fun Vector4i.toArray() = intArrayOf(x, y, z, w)

fun Vector2f.toArray() = floatArrayOf(x, y)
fun Vector3f.toArray() = floatArrayOf(x, y, z)
fun Vector4f.toArray() = floatArrayOf(x, y, z, w)

fun Vector2i.get(array: IntArray) {
    array[0] = x
    array[1] = y
}
fun Vector3i.get(array: IntArray) {
    array[0] = x
    array[1] = y
    array[2] = z
}
fun Vector4i.get(array: IntArray) {
    array[0] = x
    array[1] = y
    array[2] = z
    array[3] = w
}

fun Vector2f.get(array: FloatArray) {
    array[0] = x
    array[1] = y
}
fun Vector3f.get(array: FloatArray) {
    array[0] = x
    array[1] = y
    array[2] = z
}
fun Vector4f.get(array: FloatArray) {
    array[0] = x
    array[1] = y
    array[2] = z
    array[3] = w
}
