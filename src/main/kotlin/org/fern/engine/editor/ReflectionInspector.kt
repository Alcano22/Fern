package org.fern.engine.editor

import imgui.ImGui
import imgui.type.ImBoolean
import imgui.type.ImString
import org.fern.engine.imgui.ImGuiAssetDnD
import org.fern.engine.imgui.ImGuiEx
import org.fern.engine.renderer.Texture
import org.fern.engine.scene.component.ScriptRef
import org.fern.engine.util.Color
import org.fern.engine.util.displayStyle
import org.joml.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

typealias Drawer = (label: String, value: Any?, meta: Meta, setter: ((Any?) -> Unit)?) -> Unit

data class Meta(
    val prop: KProperty1<Any, Any?>? = null,
    val intMin: Int? = null,
    val intMax: Int? = null,
    val floatMin: Float? = null,
    val floatMax: Float? = null,
    val useSlider: Boolean = false,
    val step: Float? = null
)

object ReflectionInspector {

    private val drawers = ConcurrentHashMap<KClass<*>, Drawer>()

    private val propsCache = ConcurrentHashMap<KClass<*>, List<KProperty1<out Any, Any?>>>()

    init {
        registerDefaultDrawers()
    }

    fun registerTypeDrawer(type: KClass<*>, drawer: Drawer) {
        drawers[type] = drawer
    }

    fun inspectObject(target: Any?) {
        if (target == null) {
            ImGui.textDisabled("No selection")
            return
        }

        val cls = target::class
        val props = propsCache.computeIfAbsent(cls) { cls.memberProperties.toList() }

        for (p in props) {
            val prop = @Suppress("UNCHECKED_CAST") (p as KProperty1<Any, Any?>)

            if (shouldSkipProperty(prop)) continue
            if (prop.hasAnnotation<HideInInspector>()) continue

            val isPublic = prop.visibility == KVisibility.PUBLIC
            val isExposed = prop.hasAnnotation<ExposeInInspector>()

            if (!isPublic) {
                if (!isExposed) continue

                prop.isAccessible = true
            }

            val value = runCatching { prop.get(target) }.getOrNull()

            val mutableProp = prop as? KMutableProperty1<Any, Any?>
            if (mutableProp != null && !isPublic)
                mutableProp.isAccessible = true

            val numeric = extractNumericMeta(prop)
            val meta = Meta(
                prop,
                numeric.intMin,
                numeric.intMax,
                numeric.floatMin,
                numeric.floatMax,
                numeric.useSlider,
                numeric.step
            )

            if (mutableProp != null) {
                drawValue(prop.name, value, meta) { newVal ->
                    runCatching {
                        val coerced = coerceToType(newVal, prop.returnType.classifier as? KClass<*>)
                        mutableProp.set(target, coerced)
                    }
                }
            } else
                drawValueReadOnly(prop.name, value, meta)
        }
    }

    private fun idOnlyLabel(raw: String) = "##$raw"

    private fun shouldSkipProperty(prop: KProperty1<out Any, Any?>): Boolean {
        val n = prop.name
        if (prop.hasAnnotation<HideInInspector>())
            return true
        return n == "gameObject" ||
                n == "scene" ||
                n == "transform" ||
                n == "started" ||
                n == "enabled"
    }

    private data class NumericMeta(
        var intMin: Int? = null,
        var intMax: Int? = null,
        var floatMin: Float? = null,
        var floatMax: Float? = null,
        var useSlider: Boolean = false,
        var step: Float? = null
    )

    private fun extractNumericMeta(prop: KProperty1<Any, Any?>): NumericMeta {
        val out = NumericMeta()

        prop.findAnnotation<MinInt>()?.let { out.intMin = it.value }
        prop.findAnnotation<MaxInt>()?.let { out.intMax = it.value }
        prop.findAnnotation<RangeInt>()?.let {
            out.intMin = it.min
            out.intMax = it.max
        }
        prop.findAnnotation<SliderInt>()?.let {
            out.useSlider = true
            out.intMin = it.min
            out.intMax = it.max
        }

        prop.findAnnotation<MinFloat>()?.let { out.floatMin = it.value }
        prop.findAnnotation<MaxFloat>()?.let { out.floatMax = it.value }
        prop.findAnnotation<RangeFloat>()?.let {
            out.floatMin = it.min
            out.floatMax = it.max
        }
        prop.findAnnotation<SliderFloat>()?.let {
            out.useSlider = true
            out.floatMin = it.min
            out.floatMax = it.max
        }

        prop.findAnnotation<Step>()?.let { out.step = it.value }

        return out
    }

    private fun drawValueReadOnly(rawLabel: String, value: Any?, meta: Meta) {
        val cls = meta.prop?.returnType?.classifier as? KClass<*>
        if (value == null && cls == Texture::class) {
            drawers[Texture::class]?.invoke(rawLabel.displayStyle(), null, meta, null)
            return
        }

        if (value == null) {
            ImGuiEx.disabled { ImGui.text("null") }
            return
        }

        ImGuiEx.disabled {
            drawValueInternal(idOnlyLabel(rawLabel), value, meta, setter = null, addVisibleLabel = false)
        }
    }

    private fun drawValue(rawLabel: String, value: Any?, meta: Meta, setter: ((Any?) -> Unit)?) {
        val label = rawLabel.displayStyle()
        drawValueInternal(label, value, meta, setter, addVisibleLabel = true)
    }

    private fun drawValueInternal(
        label: String,
        value: Any?,
        meta: Meta,
        setter: ((Any?) -> Unit)?,
        addVisibleLabel: Boolean
    ) {
        val cls = meta.prop?.returnType?.classifier as? KClass<*>

        if (value == null) {
            when (cls) {
                Texture::class -> {
                    drawers[Texture::class]?.invoke(label, null, meta, setter)
                    return
                }
                ScriptRef::class -> {
                    drawers[ScriptRef::class]?.invoke(label, null, meta, setter)
                    return
                }
            }

            if (addVisibleLabel)
                ImGui.text("$label: null")
            else
                ImGui.text("null")
            return
        }

        drawers[value::class]?.let { it ->
            it(label, value, meta, setter)
            return
        }

        val assignable = drawers.entries.firstOrNull { (k, _) -> k.isInstance(value) }?.value
        if (assignable != null) {
            assignable(label, value, meta, setter)
            return
        }

        if (value::class.isSubclassOf(Enum::class)) {
            drawEnum(label, value, setter)
            return
        }

        if (ImGui.treeNode(label)) {
            inspectObject(value)
            ImGui.treePop()
        } else {
            if (addVisibleLabel)
                ImGui.text("$label: $value")
            else
                ImGui.text(value.toString())
        }
    }

    private fun drawEnum(label: String, value: Any, setter: ((Any?) -> Unit)?) {
        val enumObj = value as Enum<*>
        @Suppress("UNCHECKED_CAST")
        val constants = enumObj::class.java.enumConstants as Array<Enum<*>>
        val current = constants.indexOf(enumObj)
        if (setter == null) {
            ImGuiEx.disabled {
                ImGui.beginCombo(label, enumObj.name.displayStyle())
                ImGui.endCombo()
            }
            return
        }
        if (ImGui.beginCombo(label, enumObj.name.displayStyle())) {
            for (i in constants.indices) {
                val sel = i == current
                if (ImGui.selectable(constants[i].name.displayStyle(), sel))
                    setter(constants[i])
                if (sel)
                    ImGui.setItemDefaultFocus()
            }
            ImGui.endCombo()
        }
    }

    private fun registerDefaultDrawers() {
        registerTypeDrawer(Int::class) { label, v, meta, setter ->
            val arr = intArrayOf(v as Int)
            val speed = meta.step ?: 1f
            val hasMin = meta.intMin != null
            val hasMax = meta.intMax != null

            val changed = when {
                meta.useSlider && hasMin && hasMax ->
                    ImGui.sliderInt(label, arr, meta.intMin, meta.intMax)
                hasMin || hasMax ->
                    ImGui.dragInt(label, arr, speed, meta.intMin ?: Int.MIN_VALUE, meta.intMax ?: Int.MAX_VALUE)
                else -> ImGui.dragInt(label, arr, speed)
            }
            if (changed && setter != null)
                setter(arr[0])
        }

        registerTypeDrawer(Float::class) { label, v, meta, setter ->
            val arr = floatArrayOf(v as Float)
            val speed = meta.step ?: 0.1f
            val hasMin = meta.floatMin != null
            val hasMax = meta.floatMax != null

            val changed = when {
                meta.useSlider && hasMin && hasMax ->
                    ImGui.sliderFloat(label, arr, meta.floatMin, meta.floatMax)
                hasMin || hasMax ->
                    ImGui.dragFloat(label, arr, speed, meta.floatMin ?: -Float.MAX_VALUE, meta.floatMax ?: Float.MAX_VALUE)
                else -> ImGui.dragFloat(label, arr, speed)
            }
            if (changed && setter != null)
                setter(arr[0])
        }

        registerTypeDrawer(Boolean::class) { label, v, _, setter ->
            val b = ImBoolean(v as Boolean)
            val changed = ImGui.checkbox(label, b)
            if (changed && setter != null)
                setter(b.get())
        }

        registerTypeDrawer(String::class) { label, v, _, setter ->
            val s = v as String
            val buf = ImString(s, maxOf(64, s.length + 32))
            val changed = ImGui.inputText(label, buf)
            if (changed && setter != null)
                setter(buf.get())
        }

        registerTypeDrawer(Vector2i::class) { label, v, meta, setter ->
            val vec = v as Vector2i
            val min = meta.intMin ?: Int.MIN_VALUE
            val max = meta.intMax ?: Int.MAX_VALUE
            val speed = meta.step ?: 1f
            ImGuiEx.dragInt2(label, vec, speed, min, max) {
                if (setter != null)
                    setter(vec)
            }
        }

        registerTypeDrawer(Vector3i::class) { label, v, meta, setter ->
            val vec = v as Vector3i
            val min = meta.intMin ?: Int.MIN_VALUE
            val max = meta.intMax ?: Int.MAX_VALUE
            val speed = meta.step ?: 1f
            ImGuiEx.dragInt3(label, vec, speed, min, max) {
                if (setter != null)
                    setter(vec)
            }
        }

        registerTypeDrawer(Vector4i::class) { label, v, meta, setter ->
            val vec = v as Vector4i
            val min = meta.intMin ?: Int.MIN_VALUE
            val max = meta.intMax ?: Int.MAX_VALUE
            val speed = meta.step ?: 1f
            ImGuiEx.dragInt4(label, vec, speed, min, max) {
                if (setter != null)
                    setter(vec)
            }
        }

        registerTypeDrawer(Vector2f::class) { label, v, meta, setter ->
            val vec = v as Vector2f
            val min = meta.floatMin ?: -Float.MAX_VALUE
            val max = meta.floatMax ?: Float.MAX_VALUE
            val speed = meta.step ?: 0.1f
            ImGuiEx.dragFloat2(label, vec, speed, min, max) {
                if (setter != null)
                    setter(vec)
            }
        }

        registerTypeDrawer(Vector3f::class) { label, v, meta, setter ->
            val vec = v as Vector3f
            val min = meta.floatMin ?: -Float.MAX_VALUE
            val max = meta.floatMax ?: Float.MAX_VALUE
            val speed = meta.step ?: 0.1f
            ImGuiEx.dragFloat3(label, vec, speed, min, max) {
                if (setter != null)
                    setter(vec)
            }
        }

        registerTypeDrawer(Vector4f::class) { label, v, meta, setter ->
            val vec = v as Vector4f
            val min = meta.floatMin ?: -Float.MAX_VALUE
            val max = meta.floatMax ?: Float.MAX_VALUE
            val speed = meta.step ?: 0.1f
            ImGuiEx.dragFloat4(label, vec, speed, min, max) {
                if (setter != null)
                    setter(vec)
            }
        }

        registerTypeDrawer(Color::class) { label, v, _, setter ->
            val col = v as Color
            ImGuiEx.colorEdit4(label, col) {
                if (setter != null)
                    setter(col)
            }
        }

        registerTypeDrawer(Texture::class) { label, v, meta, setter ->
            val tex = v as? Texture
            val rawPath = tex?.filepath ?: ""
            val relShown = rawPath.removePrefix("user:")
            ImGuiEx.disabled {
                ImGui.inputText(label, ImString(relShown.ifEmpty { "<none>" }))
            }

            val frameH = ImGui.getFrameHeight()
            val slotSize = max(64f, frameH * 2.5f)
            val rounding = 5f

            val pMin = ImGui.getCursorScreenPos()
            val pMaxX = pMin.x + slotSize
            val pMaxY = pMin.y + slotSize

            ImGui.invisibleButton("##${label}_tex_slot", slotSize, slotSize)

            val dl = ImGui.getWindowDrawList()

            val texId = if (tex != null) runCatching { tex.getId() }.getOrDefault(0) else 0
            if (texId != 0) {
                dl.addImage(texId.toLong(), pMin.x, pMin.y, pMaxX, pMaxY, 0f, 1f, 1f, 0f)
                val borderCol = ImGui.getColorU32(1f, 1f, 1f, 0.15f)
                dl.addRect(pMin.x, pMin.y, pMaxX, pMaxY, borderCol, rounding)
            } else {
                val fillCol = ImGui.getColorU32(1f, 1f, 1f, 0.04f)
                val borderCol = ImGui.getColorU32(1f, 1f, 1f, 0.25f)
                dl.addRectFilled(pMin.x, pMin.y, pMaxX, pMaxY, fillCol, rounding)
                dl.addRect(pMin.x, pMin.y, pMaxX, pMaxY, borderCol, rounding)

                val hint = "Drop"
                val textW = ImGui.calcTextSizeX(hint)
                val textH = ImGui.getTextLineHeight()
                val tx = pMin.x + (slotSize - textW) * 0.5f
                val ty = pMin.y + (slotSize - textH) * 0.5f
                dl.addText(tx, ty, ImGui.getColorU32(1f, 1f, 1f, 0.35f), hint)
            }

            ImGuiAssetDnD.acceptTexture { droppedRel ->
                if (droppedRel.lowercase().endsWith(".png")) {
                    val norm = droppedRel.replace('\\', '/')
                    val newTex = runCatching { Texture.create("user:$norm") }.getOrNull()
                    if (newTex != null)
                        setter?.invoke(newTex)
                }
            }
        }

        registerTypeDrawer(ScriptRef::class) { label, v, meta, setter ->
            val ref = v as? ScriptRef
            val shown = ref?.path ?: "<none>"

            ImGuiEx.disabled {
                ImGui.inputText(label, ImString(shown))
            }

            ImGuiAssetDnD.acceptScript { droppedRel ->
                val norm = droppedRel.replace('\\', '/')
                setter?.invoke(ScriptRef(norm))
            }
        }
    }

    private fun coerceToType(value: Any?, target: KClass<*>?): Any? {
        if (value == null || target == null)
            return value

        return when (target) {
            Int::class      -> (value as? Number)?.toInt()
            Float::class    -> (value as? Number)?.toFloat()
            Boolean::class  -> value as? Boolean
            String::class   -> value.toString()
            Vector2i::class,
            Vector3i::class,
            Vector4i::class,
            Vector2f::class,
            Vector3f::class,
            Vector4f::class -> value
            else            -> value
        }
    }

}
