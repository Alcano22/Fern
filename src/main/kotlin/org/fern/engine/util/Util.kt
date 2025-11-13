package org.fern.engine.util

import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

typealias Action = () -> Unit

fun <K, V> mutableHashMapOf(): MutableMap<K, V> = HashMap()

fun <K, V> mutableHashMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V> =
    HashMap<K, V>(pairs.size).apply {
        for (p in pairs)
            put(p.first, p.second)
    }

fun <K, V> mutableHashMapOf(initialCapacity: Int = 16, loadFactor: Float = 0.75f): MutableMap<K, V> =
    HashMap(initialCapacity, loadFactor)

fun <K, V> concurrentMutableMapOf(): MutableMap<K, V> = ConcurrentHashMap()
fun <K, V> concurrentMutableMapOf(initialCapacity: Int): MutableMap<K, V> = ConcurrentHashMap(initialCapacity)

fun String.displayStyle(): String {
    if (isEmpty()) return this

    return this
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("""([a-z\d])([A-Z])"""), "$1 $2")
        .replace(Regex("""([A-Z]+)([A-Z][a-z])"""), "$1 $2")
        .trim()
        .replace(Regex("""\s+"""), " ")
        .lowercase()
        .split(' ')
        .joinToString(" ") { word ->
            if (!word.isEmpty())
                word.replaceFirstChar { it.uppercaseChar() }
            else
                ""
        }
}
