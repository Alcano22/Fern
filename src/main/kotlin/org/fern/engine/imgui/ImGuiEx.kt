package org.fern.engine.imgui

import imgui.ImFont
import imgui.ImGui
import imgui.ImVec4
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImFloat
import imgui.type.ImInt
import imgui.type.ImString
import org.fern.engine.renderer.Texture
import org.fern.engine.renderer.TextureFilter
import org.fern.engine.renderer.TextureWrap
import org.fern.engine.util.*
import org.joml.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.reflect.KMutableProperty0

object ImGuiEx {

    private val imB  = ThreadLocal.withInitial { ImBoolean() }

    private val imI1 = ThreadLocal.withInitial { ImInt() }
    private val i1   = ThreadLocal.withInitial { IntArray(1) }
    private val i2   = ThreadLocal.withInitial { IntArray(2) }
    private val i3   = ThreadLocal.withInitial { IntArray(3) }
    private val i4   = ThreadLocal.withInitial { IntArray(4) }

    private val imF1 = ThreadLocal.withInitial { ImFloat() }
    private val f1   = ThreadLocal.withInitial { FloatArray(1) }
    private val f2   = ThreadLocal.withInitial { FloatArray(2) }
    private val f3   = ThreadLocal.withInitial { FloatArray(3) }
    private val f4   = ThreadLocal.withInitial { FloatArray(4) }

    private val imS  = ThreadLocal.withInitial { ImString() }

    private val linkedStates = mutableMapOf<Int, Boolean>()

    private fun linkedKey(label: String) = ImGui.getID("Link##$label")
    private fun isLinkedFor(label: String) = linkedStates.getOrPut(linkedKey(label)) { false }
    private fun toggleLinkedFor(label: String) {
        val key = linkedKey(label)
        linkedStates[key] = !linkedStates.getOrDefault(key, false)
    }

    private object Icons {
        const val CHAIN = "internal:textures/ui/chain.png"
        const val CHAIN_BROKEN = "internal:textures/ui/chain_broken.png"
    }

    private val iconCache = mutableMapOf<String, Int>()

    private fun loadIconId(path: String): Int = iconCache.getOrPut(path) {
        try {
            val tex = Texture.create(path)
            tex.setParams(
                minFilter = TextureFilter.LINEAR,
                magFilter = TextureFilter.LINEAR,
                wrapS = TextureWrap.CLAMP_TO_EDGE,
                wrapT = TextureWrap.CLAMP_TO_EDGE
            )
            tex.getId()
        } catch (_: Exception) { 0 }
    }

    fun inputInt(
        label: String,
        value: KMutableProperty0<Int>,
        step: Int = 1,
        onChange: Action = {}
    ) {
        val tmp = imI1.get()
        tmp.set(value.get())
        if (ImGui.inputInt(label, tmp, step)) {
            value.set(tmp.get())
            onChange()
        }
    }

    fun dragInt(
        label: String,
        value: KMutableProperty0<Int>,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = i1.get()
        tmp[0] = value.get()
        if (ImGui.dragInt(label, tmp, speed, min, max)) {
            value.set(tmp[0])
            onChange()
        }
    }

    fun sliderInt(
        label: String,
        value: KMutableProperty0<Int>,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = i1.get()
        tmp[0] = value.get()
        if (ImGui.sliderInt(label, tmp, min, max)) {
            value.set(tmp[0])
            onChange()
        }
    }

    fun inputFloat(
        label: String,
        value: KMutableProperty0<Float>,
        step: Float = 0.1f,
        onChange: Action = {}
    ) {
        val tmp = imF1.get()
        tmp.set(value.get())
        if (ImGui.inputFloat(label, tmp, step)) {
            value.set(tmp.get())
            onChange()
        }
    }

    fun dragFloat(
        label: String,
        value: KMutableProperty0<Float>,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = f1.get()
        tmp[0] = value.get()
        if (ImGui.dragFloat(label, tmp, speed, min, max)) {
            value.set(tmp[0])
            onChange()
        }
    }

    fun sliderFloat(
        label: String,
        value: KMutableProperty0<Float>,
        min: Float,
        max: Float,
        onChange: Action = {}
    ) {
        val tmp = f1.get()
        tmp[0] = value.get()
        if (ImGui.sliderFloat(label, tmp, min, max)) {
            value.set(tmp[0])
            onChange()
        }
    }

    fun checkbox(
        label: String,
        value: KMutableProperty0<Boolean>,
        onChange: Action = {}
    ) {
        val tmp = imB.get()
        tmp.set(value.get())
        if (ImGui.checkbox(label, tmp)) {
            value.set(tmp.get())
            onChange()
        }
    }

    fun inputInt2(
        label: String,
        value: Vector2i,
        step: Int = 1,
        onChange: Action = {}
    ) {
        val tmp = i2.get()
        value.get(tmp)
        if (ImGui.inputInt2(label, tmp, step)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragInt2(
        label: String,
        value: Vector2i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = i2.get()
        value.get(tmp)
        if (ImGui.dragInt2(label, tmp, speed, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragInt2Linked(
        label: String,
        value: Vector2i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) = dragIntLinkedGeneric(
        label,
        2,
        { arr -> value.get(arr) },
        { arr -> value.set(arr) },
        { arr -> ImGui.dragInt2("##dragInt2_$label", arr, speed, min, max) },
        onChange
    )

    fun sliderInt2(
        label: String,
        value: Vector2i,
        min: Int,
        max: Int,
        onChange: Action = {}
    ) {
        val tmp = i2.get()
        value.get(tmp)
        if (ImGui.sliderInt2(label, tmp, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun inputInt3(
        label: String,
        value: Vector3i,
        step: Int = 1,
        onChange: Action = {}
    ) {
        val tmp = i3.get()
        value.get(tmp)
        if (ImGui.inputInt3(label, tmp, step)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragInt3(
        label: String,
        value: Vector3i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = i3.get()
        value.get(tmp)
        if (ImGui.dragInt3(label, tmp, speed, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragInt3Linked(
        label: String,
        value: Vector3i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) = dragIntLinkedGeneric(
        label,
        3,
        { arr -> value.get(arr) },
        { arr -> value.set(arr) },
        { arr -> ImGui.dragInt3("##dragInt3_$label", arr, speed, min, max) },
        onChange
    )

    fun sliderInt3(
        label: String,
        value: Vector3i,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = i3.get()
        value.get(tmp)
        if (ImGui.sliderInt3(label, tmp, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun inputInt4(
        label: String,
        value: Vector4i,
        step: Int = 1,
        onChange: Action = {}
    ) {
        val tmp = i4.get()
        value.get(tmp)
        if (ImGui.inputInt4(label, tmp, step)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragInt4(
        label: String,
        value: Vector4i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = i4.get()
        value.get(tmp)
        if (ImGui.dragInt4(label, tmp, speed, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragInt4Linked(
        label: String,
        value: Vector4i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) = dragIntLinkedGeneric(
        label,
        4,
        { arr -> value.get(arr) },
        { arr -> value.set(arr) },
        { arr -> ImGui.dragInt4("##dragInt4_$label", arr, speed, min, max) },
        onChange
    )

    fun sliderInt4(
        label: String,
        value: Vector4i,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = i4.get()
        value.get(tmp)
        if (ImGui.sliderInt4(label, tmp, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun inputFloat2(
        label: String,
        value: Vector2f,
        onChange: Action = {}
    ) {
        val tmp = f2.get()
        value.get(tmp)
        if (ImGui.inputFloat2(label, tmp)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragFloat2(
        label: String,
        value: Vector2f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = f2.get()
        value.get(tmp)
        if (ImGui.dragFloat2(label, tmp, speed, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragFloat2Linked(
        label: String,
        value: Vector2f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        onChange: Action = {}
    ) = dragFloatLinkedGeneric(
        label,
        2,
        { arr -> value.get(arr) },
        { arr -> value.set(arr) },
        { arr -> ImGui.dragFloat2("##dragFloat2_$label", arr, speed, min, max) },
        onChange
    )

    fun sliderFloat2(
        label: String,
        value: Vector2f,
        min: Float,
        max: Float,
        onChange: Action = {}
    ) {
        val tmp = f2.get()
        value.get(tmp)
        if (ImGui.sliderFloat2(label, tmp, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun inputFloat3(
        label: String,
        value: Vector3f,
        onChange: Action = {}
    ) {
        val tmp = f3.get()
        value.get(tmp)
        if (ImGui.inputFloat3(label, tmp)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragFloat3(
        label: String,
        value: Vector3f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = f3.get()
        value.get(tmp)
        if (ImGui.dragFloat3(label, tmp, speed, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragFloat3Linked(
        label: String,
        value: Vector3f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        onChange: Action = {}
    ) = dragFloatLinkedGeneric(
        label,
        3,
        { arr -> value.get(arr) },
        { arr -> value.set(arr) },
        { arr -> ImGui.dragFloat3("##dragFloat3_$label", arr, speed, min, max) },
        onChange
    )

    fun sliderFloat3(
        label: String,
        value: Vector3f,
        min: Float,
        max: Float,
        onChange: Action = {}
    ) {
        val tmp = f3.get()
        value.get(tmp)
        if (ImGui.sliderFloat3(label, tmp, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun inputFloat4(
        label: String,
        value: Vector4f,
        onChange: Action = {}
    ) {
        val tmp = f4.get()
        value.get(tmp)
        if (ImGui.inputFloat4(label, tmp)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragFloat4(
        label: String,
        value: Vector4f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        onChange: Action = {}
    ) {
        val tmp = f4.get()
        value.get(tmp)
        if (ImGui.dragFloat4(label, tmp, speed, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun dragFloat4Linked(
        label: String,
        value: Vector4f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        onChange: Action = {}
    ) = dragFloatLinkedGeneric(
        label,
        4,
        { arr -> value.get(arr) },
        { arr -> value.set(arr) },
        { arr -> ImGui.dragFloat4("##dragFloat4_$label", arr, speed, min, max) },
        onChange
    )

    fun sliderFloat4(
        label: String,
        value: Vector4f,
        min: Float,
        max: Float,
        onChange: Action = {}
    ) {
        val tmp = f4.get()
        value.get(tmp)
        if (ImGui.sliderFloat4(label, tmp, min, max)) {
            value.set(tmp)
            onChange()
        }
    }

    fun colorEdit3(
        label: String,
        value: Color,
        onChange: Action = {}
    ) {
        val tmp = f3.get()
        value.get(tmp)
        if (ImGui.colorEdit3(label, tmp)) {
            value.set(tmp)
            onChange()
        }
    }

    fun colorEdit4(
        label: String,
        value: Color,
        onChange: Action = {}
    ) {
        val tmp = f4.get()
        value.get(tmp)
        if (ImGui.colorEdit4(label, tmp)) {
            value.set(tmp)
            onChange()
        }
    }

    fun colorPicker3(
        label: String,
        value: Color,
        onChange: Action = {}
    ) {
        val tmp = f3.get()
        value.get(tmp)
        if (ImGui.colorPicker3(label, tmp)) {
            value.set(tmp)
            onChange()
        }
    }

    fun colorPicker4(
        label: String,
        value: Color,
        onChange: Action = {}
    ) {
        val tmp = f4.get()
        value.get(tmp)
        if (ImGui.colorPicker4(label, tmp)) {
            value.set(tmp)
            onChange()
        }
    }

    fun inputText(
        label: String,
        value: KMutableProperty0<String>,
        hint: String? = null,
        flags: Int = ImGuiInputTextFlags.None,
        onChange: Action = {}
    ) {
        val tmp = imS.get()
        tmp.set(value.get())
        if (hint == null) {
            if (ImGui.inputText(label, tmp, flags)) {
                value.set(tmp.get())
                onChange()
            }
        } else {
            if (ImGui.inputTextWithHint(label, hint, tmp, flags)) {
                value.set(tmp.get())
                onChange()
            }
        }
    }

    fun inputTextMultiline(
        label: String,
        value: KMutableProperty0<String>,
        width: Float? = null,
        height: Float? = null,
        flags: Int = ImGuiInputTextFlags.None,
        onChange: Action = {}
    ) {
        val tmp = imS.get()
        tmp.set(value.get())
        val changed = if (width != null && height != null)
            ImGui.inputTextMultiline(label, tmp, width, height, flags)
        else
            ImGui.inputTextMultiline(label, tmp, flags)
        if (changed) {
            value.set(tmp.get())
            onChange()
        }
    }

    inline fun <reified T : Enum<T>> enumCombo(
        label: String,
        value: KMutableProperty0<T>,
        noinline onChange: Action = {}
    ) {
        val current = value.get()
        val preview = current.name.displayStyle()
        if (ImGui.beginCombo(label, preview)) {
            val constants = enumValues<T>()
            for (c in constants) {
                val name = c.name.displayStyle()
                val selected = c == current
                if (ImGui.selectable(name, selected)) {
                    value.set(c)
                    onChange()
                }
                if (selected)
                    ImGui.setItemDefaultFocus()
            }
            ImGui.endCombo()
        }
    }

    inline fun button(
        label: String,
        sizeX: Float = 0f,
        sizeY: Float = 0f,
        block: () -> Unit
    ) {
        var sx = sizeX
        var sy = sizeY

        if (sx == 0f)
            sx = ImGui.calcTextSizeX(label) + ImGui.getStyle().framePaddingX * 2
        if (sy == 0f)
            sy = ImGui.getFrameHeight()

        if (ImGui.button(label, sx, sy))
            block()
    }

    inline fun imageButton(
        id: String,
        texId: Int,
        sizeX: Float? = null,
        sizeY: Float? = null,
        uv0X: Float = 0f,
        uv0Y: Float = 1f,
        uv1X: Float = 1f,
        uv1Y: Float = 0f,
        bgCol: Color? = null,
        block: () -> Unit
    ) {
        var sx = sizeX
        var sy = sizeY

        val side = ImGui.getFrameHeight()
        if (sx == null)
            sx = side
        if (sy == null)
            sy = side

        if (bgCol == null) {
            if (ImGui.imageButton(id, texId.toLong(), sx, sy, uv0X, uv0Y, uv1X, uv1Y))
                block()
        } else {
            if (ImGui.imageButton(
                id,
                texId.toLong(),
                sx, sy,
                uv0X, uv0Y, uv1X, uv1Y,
                bgCol.r, bgCol.g, bgCol.b, bgCol.a
            ))
                block()
        }
    }

    fun spacing(n: Int = 1) = repeat(n) { ImGui.spacing() }

    inline fun indented(indent: Float, block: () -> Unit) {
        ImGui.indent(indent)
        try {
            block()
        } finally {
            ImGui.unindent(indent)
        }
    }

    sealed interface StyleVarSpec {
        data class One(val idx: Int, val v: Float) : StyleVarSpec
        data class Two(val idx: Int, val x: Float, val y: Float) : StyleVarSpec
    }
    fun sv(idx: Int, v: Float) = StyleVarSpec.One(idx, v)
    fun sv(idx: Int, x: Float, y: Float) = StyleVarSpec.Two(idx, x, y)

    sealed interface ColorVarSpec {
        data class Four(val idx: Int, val r: Float, val g: Float, val b: Float, val a: Float) : ColorVarSpec
        data class Packed(val idx: Int, val col: Int) : ColorVarSpec
    }
    fun col(idx: Int, color: ImVec4) = ColorVarSpec.Four(idx, color.x, color.y, color.z, color.w)
    fun col(idx: Int, color: Color) = ColorVarSpec.Four(idx, color.r, color.g, color.b, color.a)
    fun col(idx: Int, r: Float, g: Float, b: Float, a: Float) = ColorVarSpec.Four(idx, r, g, b, a)
    fun col(idx: Int, color: Int) = ColorVarSpec.Packed(idx, color)

    inline fun withStyle(vararg vars: StyleVarSpec, block: Action) {
        vars.forEach { v ->
            when (v) {
                is StyleVarSpec.One -> ImGui.pushStyleVar(v.idx, v.v)
                is StyleVarSpec.Two -> ImGui.pushStyleVar(v.idx, v.x, v.y)
            }
        }
        try {
            block()
        } finally {
            ImGui.popStyleVar(vars.size)
        }
    }

    inline fun withColor(vararg vars: ColorVarSpec, block: Action) {
        vars.forEach { v ->
            when (v) {
                is ColorVarSpec.Four   -> ImGui.pushStyleColor(v.idx, v.r, v.g, v.b, v.a)
                is ColorVarSpec.Packed -> ImGui.pushStyleColor(v.idx, v.col)
            }
        }
        try {
            block()
        } finally {
            ImGui.popStyleColor(vars.size)
        }
    }

    inline fun withStyleAndColor(
        styles: Array<out StyleVarSpec> = emptyArray(),
        colors: Array<out ColorVarSpec> = emptyArray(),
        block: Action
    ) {
        styles.forEach { v ->
            when (v) {
                is StyleVarSpec.One -> ImGui.pushStyleVar(v.idx, v.v)
                is StyleVarSpec.Two -> ImGui.pushStyleVar(v.idx, v.x, v.y)
            }
        }
        colors.forEach { v ->
            when (v) {
                is ColorVarSpec.Four   -> ImGui.pushStyleColor(v.idx, v.r, v.g, v.b, v.a)
                is ColorVarSpec.Packed -> ImGui.pushStyleColor(v.idx, v.col)
            }
        }
        try {
            block()
        } finally {
            ImGui.popStyleColor(colors.size)
            ImGui.popStyleVar(styles.size)
        }
    }

    class StyleColorBuilder {
        val styles = mutableListOf<StyleVarSpec>()
        val colors = mutableListOf<ColorVarSpec>()

        fun style(idx: Int, v: Float) { styles.add(sv(idx, v)) }
        fun style(idx: Int, x: Float, y: Float) { styles.add(sv(idx, x, y)) }

        fun color(idx: Int, col: ImVec4) { colors.add(col(idx, col)) }
        fun color(idx: Int, col: Color) { colors.add(col(idx, col)) }
        fun color(idx: Int, r: Float, g: Float, b: Float, a: Float) { colors.add(col(idx, r, g, b, a)) }
        fun color(idx: Int, rgba: Int) { colors.add(col(idx, rgbaToAbgr(rgba))) }

        private fun rgbaToAbgr(rgba: Int): Int {
            val r = (rgba ushr 24) and 0xFF
            val g = (rgba ushr 16) and 0xFF
            val b = (rgba ushr 8) and 0xFF
            val a = rgba and 0xFF
            return (a shl 24) or (b shl 16) or (g shl 8) or r
        }
    }

    inline fun withStyle(
        vars: StyleColorBuilder.() -> Unit,
        body: Action
    ) {
        val b = StyleColorBuilder().apply(vars)
        b.styles.forEach {
            when (it) {
                is StyleVarSpec.One -> ImGui.pushStyleVar(it.idx, it.v)
                is StyleVarSpec.Two -> ImGui.pushStyleVar(it.idx, it.x, it.y)
            }
        }
        b.colors.forEach {
            when (it) {
                is ColorVarSpec.Four   -> ImGui.pushStyleColor(it.idx, it.r, it.g, it.b, it.a)
                is ColorVarSpec.Packed -> ImGui.pushStyleColor(it.idx, it.col)
            }
        }

        try {
            body()
        } finally {
            ImGui.popStyleColor(b.colors.size)
            ImGui.popStyleVar(b.styles.size)
        }
    }

    inline fun withFont(font: ImFont, block: () -> Unit) {
        ImGui.pushFont(font)
        try {
            block()
        } finally {
            ImGui.popFont()
        }
    }

    fun disabled(disabled: Boolean = true, block: Action) {
        if (disabled)
            ImGui.beginDisabled()
        block()
        if (disabled)
            ImGui.endDisabled()
    }

    inline fun collapsingHeader(
        label: String,
        flags: Int = ImGuiTreeNodeFlags.None,
        body: Action
    ) {
        if (ImGui.collapsingHeader(label, flags))
            body()
    }

    fun window(
        title: String,
        open: KMutableProperty0<Boolean>? = null,
        flags: Int = ImGuiWindowFlags.None,
        body: () -> Unit
    ) {
        val imOpen = open?.let { imB.get().also { ib -> ib.set(it.get()) } }

        val began = if (imOpen != null)
            ImGui.begin(title, imOpen, flags)
        else
            ImGui.begin(title, flags)

        try {
            if (began)
                body()
        } finally {
            ImGui.end()
        }

        if (imOpen != null)
            open.set(imOpen.get())
    }

    private fun dragIntLinkedGeneric(
        label: String,
        componentCount: Int,
        fetchInto: (IntArray) -> Unit,
        writeBack: (IntArray) -> Unit,
        drawImGui: (IntArray) -> Boolean,
        onChange: Action
    ) {
        ImGui.pushID(label)

        val style = ImGui.getStyle()
        val baseWidth = ImGui.calcItemWidth()
        val frameHeight = ImGui.getFrameHeight()
        val gap = style.itemInnerSpacingX

        val imgSide = max(1f, frameHeight - 2f * style.framePaddingY)
        val totalBtnWidth = imgSide + 2f * style.framePaddingX
        var dragWidth = baseWidth - (totalBtnWidth + gap)
        if (dragWidth < 10f)
            dragWidth = 10f

        val linked = isLinkedFor(label)
        val iconPath = if (linked) Icons.CHAIN else Icons.CHAIN_BROKEN
        val iconId = loadIconId(iconPath)
        imageButton("##link_$label", iconId, imgSide, imgSide, 0f, 1f, 1f, 0f) {
            toggleLinkedFor(label)
        }

        ImGui.sameLine(0f, gap)

        val tmp = when (componentCount) {
            2 -> i2.get()
            3 -> i3.get()
            4 -> i4.get()
            else -> error("Unsupported int vector size: $componentCount")
        }
        val old = IntArray(componentCount)
        fetchInto(old)
        fetchInto(tmp)

        ImGui.setNextItemWidth(dragWidth)
        val changed = drawImGui(tmp)

        ImGui.sameLine()
        ImGui.text(label)

        if (changed) {
            if (isLinkedFor(label)) {
                val deltas = IntArray(componentCount) { tmp[it] - old[it] }
                var primary = 0
                var best = abs(deltas[0])
                for (i in 1 until componentCount) {
                    val a = abs(deltas[i])
                    if (a <= best) continue

                    best = a
                    primary = i
                }
                val d = deltas[primary]
                if (d != 0) {
                    for (i in 0 until componentCount) {
                        if (i == primary) continue

                        tmp[i] = old[i] + d
                    }
                }
            }
            writeBack(tmp)
            onChange()
        }

        ImGui.popID()
    }

    private fun dragFloatLinkedGeneric(
        label: String,
        componentCount: Int,
        fetchInto: (FloatArray) -> Unit,
        writeBack: (FloatArray) -> Unit,
        drawImGui: (FloatArray) -> Boolean,
        onChange: Action
    ) {
        ImGui.pushID(label)

        val style = ImGui.getStyle()
        val baseWidth = ImGui.calcItemWidth()
        val frameHeight = ImGui.getFrameHeight()
        val gap = style.itemInnerSpacingX

        val imgSide = max(1f, frameHeight - 2f * style.framePaddingY)
        val totalBtnWidth = imgSide + 2f * style.framePaddingX
        var dragWidth = baseWidth - (totalBtnWidth + gap)
        if (dragWidth < 10f)
            dragWidth = 10f

        val linked = isLinkedFor(label)
        val iconPath = if (linked) Icons.CHAIN else Icons.CHAIN_BROKEN
        val iconId = loadIconId(iconPath)
        imageButton("##link_$label", iconId, imgSide, imgSide, 0f, 1f, 1f, 0f) {
            toggleLinkedFor(label)
        }

        ImGui.sameLine(0f, gap)

        val tmp = when (componentCount) {
            2 -> f2.get()
            3 -> f3.get()
            4 -> f4.get()
            else -> error("Unsupported int vector size: $componentCount")
        }
        val old = FloatArray(componentCount)
        fetchInto(old)
        fetchInto(tmp)

        ImGui.setNextItemWidth(dragWidth)
        val changed = drawImGui(tmp)

        ImGui.sameLine()
        ImGui.text(label)

        if (changed) {
            if (isLinkedFor(label)) {
                val deltas = FloatArray(componentCount) { tmp[it] - old[it] }
                var primary = 0
                var best = abs(deltas[0])
                for (i in 1 until componentCount) {
                    val a = abs(deltas[i])
                    if (a <= best) continue

                    best = a
                    primary = i
                }
                val d = deltas[primary]
                if (d != 0f) {
                    for (i in 0 until componentCount) {
                        if (i == primary) continue

                        tmp[i] = old[i] + d
                    }
                }
            }
            writeBack(tmp)
            onChange()
        }

        ImGui.popID()
    }

}