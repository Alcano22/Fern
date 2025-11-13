package org.fern.engine.logger

import mu.KLogger
import mu.KotlinLogging
import kotlin.reflect.KClass

fun Any.logger(info: String = ""): KLogger {
    val cls = this::class
    val shortInfo = info.trim().let {
        if (it.isEmpty()) "" else {
            val name = it
                .substringAfterLast('/')
                .substringAfterLast('\\')
            " ($name)"
        }
    }

    val baseName = try {
        if (cls.isCompanion) {
            val enclosing = cls.java.enclosingClass ?: cls.java.declaringClass
            enclosing?.simpleName ?: fallbackName(cls)
        } else
            cls.simpleName ?: fallbackName(cls)
    } catch (_: Throwable) {
        fallbackName(cls)
    }

    return KotlinLogging.logger("$baseName$shortInfo")
}

private fun fallbackName(cls: KClass<*>) = cls.simpleName
    ?: cls.qualifiedName?.substringAfterLast('.')
    ?: cls.java.name.substringAfterLast('.')
