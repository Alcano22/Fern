package org.fern.engine.scene.component

import io.github.classgraph.ClassGraph
import org.fern.engine.logger.logger
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisterComponent(val category: String = "General")

data class ComponentInfo(
    val cls: KClass<out Component>,
    val name: String,
    val category: String,
    val isSingle: Boolean
)

object ComponentRegistry {

    private val logger = logger()

    private val list = mutableListOf<ComponentInfo>()
    private val byCategory = linkedMapOf<String, MutableList<ComponentInfo>>()

    fun clear() {
        list.clear()
        byCategory.clear()
    }

    fun all() = list.toList()

    fun categories() = byCategory.mapValues { it.value.toList() }

    fun register(
        cls: KClass<out Component>,
        category: String = "General",
        displayName: String? = null
    ) {
        if (list.any { it.cls == cls }) return

        val derivedName = displayName ?: (cls.simpleName ?: "Component")
        val single = cls.hasAnnotation<SingleComponent>()
        val info = ComponentInfo(cls, derivedName, category, single)
        list.add(info)
        byCategory.getOrPut(category) { mutableListOf() }.add(info)
    }

    fun scanAndRegister(packageName: String) {
        val scanResult = ClassGraph()
            .enableClassInfo()
            .acceptPackages(packageName)
            .scan()

        val cis = scanResult.getSubclasses(Component::class.java)
        for (ci in cis) {
            if (!ci.isStandardClass || ci.isAbstract) continue

            try {
                val cls = ci.loadClass()
                @Suppress("UNCHECKED_CAST")
                val kCls = cls.kotlin as KClass<out Component>

                val hasNoArgCtor = runCatching { cls.getDeclaredConstructor() }.isSuccess
                if (!hasNoArgCtor) continue

                val reg = kCls.findAnnotation<RegisterComponent>()
                val category = reg?.category ?: "General"

                val displayName = kCls.simpleName ?: "Component"

                register(kCls, category, displayName)
            } catch (t: Throwable) {
                logger.warn(t) { "Failed to register component from class ${ci.name}" }
            }
        }

        list.sortBy { it.name.lowercase() }
        byCategory.forEach { (_, items) -> items.sortBy { it.name.lowercase() } }
    }

}