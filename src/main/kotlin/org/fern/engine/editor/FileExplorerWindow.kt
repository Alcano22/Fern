package org.fern.engine.editor

import imgui.ImGui
import imgui.flag.*
import org.fern.engine.imgui.ImGuiAssetDnD
import org.fern.engine.imgui.ImGuiEx
import org.fern.engine.renderer.Texture
import org.fern.engine.util.Color
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.math.floor
import kotlin.math.max

class FileExplorerWindow(
    private val root: Path = Paths.get("assets"),
    private val onSelect: (String?) -> Unit = {}
) : EditorWindow(
    id = "file_explorer",
    title = "File Explorer",
    closeable = true,
    hasMenuBar = false,
    hasContextMenu = false,
    category = "Window",
    includeInWindowMenu = true
) {

    companion object {
        private const val LEFT_PANE_WIDTH = 260f
        private const val THUMB_SIZE = 80f
        private const val CELL_PADDING = 12f
        private const val CELL_WIDTH = THUMB_SIZE + CELL_PADDING
        private const val LABEL_MAX_CHARS = 24
    }

    private var currentDir: Path = root
    private var selectedEntry: Path? = null

    override fun init() {
        currentDir = currentDir.normalize()
        if (!Files.exists(root))
            runCatching { Files.createDirectories(root) }
        if (!isUnderRoot(currentDir))
            currentDir = root
    }

    override fun renderContent() {
        if (ImGui.beginChild("##left_pane", LEFT_PANE_WIDTH, 0f, true))
            renderTree()
        ImGui.endChild()

        ImGui.sameLine()

        if (ImGui.beginChild("##right_pane", 0f, 0f, false))
            renderRightPane()
        ImGui.endChild()
    }

    private fun renderTree() {
        ImGui.pushID("root")
        val rootFlags = ImGuiTreeNodeFlags.OpenOnArrow or
                ImGuiTreeNodeFlags.OpenOnDoubleClick or
                ImGuiTreeNodeFlags.DefaultOpen or
                ImGuiTreeNodeFlags.SpanFullWidth or
                (if (currentDir == root) ImGuiTreeNodeFlags.Selected else 0)
        val rootOpened = ImGui.treeNodeEx("assets", rootFlags)
        if (ImGui.isItemClicked())
            selectDirectory(root)
        if (rootOpened) {
            drawDirectoryChildren(root)
            ImGui.treePop()
        }
        ImGui.popID()
    }

    private fun drawDirectoryChildren(dir: Path) {
        val subDirs = listChildren(dir, onlyDirectories = true)
        for (child in subDirs) {
            ImGui.pushID(child.toString())

            val hasChildren = hasSubDirectories(child)
            val flags = ImGuiTreeNodeFlags.OpenOnArrow or
                    ImGuiTreeNodeFlags.OpenOnDoubleClick or
                    ImGuiTreeNodeFlags.SpanFullWidth or
                    (if (currentDir == child) ImGuiTreeNodeFlags.Selected else 0) or
                    (if (!hasChildren) ImGuiTreeNodeFlags.Leaf or ImGuiTreeNodeFlags.NoTreePushOnOpen else 0)

            val label = child.name.ifEmpty { child.toString() }
            val opened = ImGui.treeNodeEx(label, flags)
            if (ImGui.isItemClicked())
                selectDirectory(child)
            if (opened && hasChildren) {
                drawDirectoryChildren(child)
                ImGui.treePop()
            }

            ImGui.popID()
        }
    }

    private fun hasSubDirectories(dir: Path): Boolean {
        if (!Files.isDirectory(dir))
            return false

        Files.newDirectoryStream(dir).use { ds ->
            for (p in ds) {
                if (p.isDirectory() && !p.name.startsWith('.'))
                    return true
            }
        }
        return false
    }

    private fun selectDirectory(path: Path) {
        val p = path.normalize()
        if (Files.isDirectory(p) && isUnderRoot(p)) {
            currentDir = p
            selectedEntry = null
        }
    }

    private fun renderRightPane() {
        val footerH = ImGui.getTextLineHeightWithSpacing() +
                ImGui.getStyle().framePaddingY * 2f +
                ImGui.getStyle().itemSpacingY
        val availY = ImGui.getContentRegionAvailY()
        val topH = max(0f, availY - footerH)

        if (ImGui.beginChild("##right_top", 0f, topH, false)) {
            renderToolbar()
            ImGuiEx.spacing()
            ImGui.separator()
            ImGuiEx.spacing()
            renderGrid()
        }
        ImGui.endChild()

        ImGui.separator()

        if (ImGui.beginChild("##right_footer", 0f, 0f, false))
            renderFooter()
        ImGui.endChild()
    }

    private fun renderToolbar() {
        renderBreadcrumbs()

        ImGui.sameLine()

        ImGuiEx.button("Up") {
            val parent = currentDir.parent
            if (parent != null && isUnderRoot(parent))
                selectDirectory(parent)
            else
                selectDirectory(root)
        }
    }

    private fun renderBreadcrumbs() {
        val rootClicked = ImGui.smallButton("assets")
        if (rootClicked)
            selectDirectory(root)

        ImGui.sameLine()
        ImGui.textDisabled("/")
        ImGui.sameLine()

        val rel = runCatching { root.relativize(currentDir) }.getOrNull()
        if (rel == null || rel.toString().isEmpty()) {
            ImGui.textDisabled(".")
            return
        }

        var pathSoFar = root
        val it = rel.iterator()
        var first = true
        while (it.hasNext()) {
            val seg = it.next().toString()
            if (!first) {
                ImGui.sameLine()
                ImGui.textDisabled("/")
                ImGui.sameLine()
            }
            first = false

            pathSoFar = pathSoFar.resolve(seg)
            if (ImGui.smallButton(seg))
                selectDirectory(pathSoFar)
        }
    }

    private fun renderGrid() {
        val contents = listChildren(currentDir, onlyDirectories = false)

        val sorted = contents.sortedWith(
            compareBy<Path> { !Files.isDirectory(it) }.thenBy { it.name.lowercase() }
        ).filter { it.extension != "meta" }

        val avail = ImGui.getContentRegionAvailX()
        val perRow = max(1, floor((avail + CELL_PADDING) / (CELL_WIDTH)).toInt())

        val tableFlags = ImGuiTableFlags.SizingStretchSame or
                ImGuiTableFlags.NoPadOuterX
                ImGuiTableFlags.NoPadInnerX

        if (ImGui.beginTable("##grid", perRow, tableFlags)) {
            for ((i, entry) in sorted.withIndex()) {
                ImGui.tableNextColumn()
                ImGui.pushID(i)
                drawGridCell(entry)
                ImGui.popID()
            }
            ImGui.endTable()
        }
    }

    private fun drawGridCell(path: Path) {
        val isDir = Files.isDirectory(path)
        val fileName = path.name.ifEmpty { path.nameWithoutExtension }
        val ext = path.extension.lowercase()
        val texId = when {
            isDir -> IconCache.id(IconCache.FOLDER)
            else -> IconCache.idForExtension(ext)
        }

        val relPath = try {
            root.relativize(path).toString().replace('\\', '/')
        } catch (_: Exception) {
            path.fileName.toString()
        }

        val label = elide(fileName, LABEL_MAX_CHARS)
        val textH = ImGui.getTextLineHeightWithSpacing()
        val cellW = ImGui.getContentRegionAvailX()
        val cellH = THUMB_SIZE + textH + 6f

        val startPos = ImGui.getCursorPos()
        val selected = selectedEntry == path
        ImGuiEx.withStyle(
            {
                style(ImGuiStyleVar.FramePadding, 4f, 4f)

                if (selected) {
                    val colHeader = ImGui.getStyleColorVec4(ImGuiCol.Header)
                    color(ImGuiCol.HeaderHovered, colHeader)
                    color(ImGuiCol.HeaderActive, colHeader)
                } else {
                    color(ImGuiCol.Header, Color.TRANSPARENT)
                    color(ImGuiCol.HeaderHovered, Color.TRANSPARENT)
                    color(ImGuiCol.HeaderActive, Color.TRANSPARENT)
                }
            }
        ) {
            if (ImGui.selectable("##cell", selected, cellW, cellH)) {
                selectedEntry = path
                onSelect(if (!isDir) relPath else null)
            }
        }

        if (isDir) {
            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left))
                selectDirectory(path)
        } else {
            when (ext) {
                "png" -> ImGuiAssetDnD.sourceTexture(relPath)
                "script" -> ImGuiAssetDnD.sourceScript(relPath)
            }
        }

        val iconX = startPos.x + max(0f, (cellW - THUMB_SIZE) / 2f)
        val iconY = startPos.y + 2f
        ImGui.setCursorPos(iconX, iconY)
        ImGui.image(texId.toLong(), THUMB_SIZE, THUMB_SIZE, 0f, 1f, 1f, 0f)

        val textWidth = ImGui.calcTextSizeX(label)
        val textX = startPos.x + max(0f, (cellW - textWidth) / 2f)
        val textY = iconY + THUMB_SIZE + 2f
        ImGui.setCursorPos(textX, textY)
        ImGui.text(label)

        ImGui.setCursorPos(startPos.x, startPos.y + cellH)
    }

    private fun renderFooter() {
        val sel = selectedEntry
        if (sel == null) {
            ImGui.textDisabled("No selection")
            return
        }

        val isDir = Files.isDirectory(sel)
        val rel = runCatching { root.relativize(sel).toString() }.getOrElse { sel.toAbsolutePath().toString() }

        if (isDir) {
            val count = runCatching { Files.list(sel).use { it.count() } }.getOrDefault(0L)
            ImGui.textUnformatted("$rel - Folder ($count items)")
        } else {
            val size = runCatching { Files.size(sel) }.getOrDefault(-1L)
            val sizeStr = if (size >= 0) humanSize(size) else "?"
            ImGui.textUnformatted("$rel - $sizeStr")
        }

        ImGui.sameLine()
        if (ImGui.smallButton("Copy"))
            ImGui.setClipboardText(rel)

        ImGui.sameLine()
        ImGui.textDisabled("|")

        ImGui.sameLine()
        if (ImGui.smallButton("Reveal"))
            revealInExplorer(sel)
    }

    private fun listChildren(dir: Path, onlyDirectories: Boolean): List<Path> {
        if (!Files.exists(dir) || !Files.isDirectory(dir))
            return emptyList()
        return Files.list(dir).use { stream ->
            stream
                .filter { p ->
                    val name = p.name
                    if (name.isEmpty() || name.startsWith("."))
                        return@filter false
                    if (onlyDirectories)
                        Files.isDirectory(p)
                    else
                        true
                }
                .toList()
        }
    }

    private fun isUnderRoot(path: Path): Boolean {
        val base = path.toAbsolutePath().normalize()
        val candidate = path.toAbsolutePath().normalize()
        return candidate.startsWith(base)
    }

    private fun elide(text: String, maxLen: Int): String {
        if (text.length <= maxLen)
            return text
        if (maxLen <= 3)
            return text.take(maxLen)
        val keep = maxLen - 3
        return text.take(keep) + "..."
    }

    private fun humanSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var b = bytes.toDouble()
        var i = 0
        while (b >= 1024.0 && i < units.size - 1) {
            b /= 1024.0
            i++
        }
        return String.format("%.1f %s", b, units[i])
    }

    private fun revealInExplorer(path: Path) {
        val file = path.toFile()
        if (!Desktop.isDesktopSupported())
            return

        if (file.isDirectory) {
            Desktop.getDesktop().open(file)
        } else
            Desktop.getDesktop().open(file.parentFile)
    }

    private object IconCache {
        const val FOLDER = "internal:textures/ui/folder.png"
        const val FILE_GENERIC = "internal:textures/ui/file.png"

        private val extMap = mapOf(
            "png"    to "internal:textures/ui/file_image.png",
            "jpg"    to "internal:textures/ui/file_image.png",
            "jpeg"   to "internal:textures/ui/file_image.png",
            "txt"    to "internal:textures/ui/file_text.png",
            "md"     to "internal:textures/ui/file_text.png",
            "json"   to "internal:textures/ui/file_text.png",
            "yml"    to "internal:textures/ui/file_text.png",
            "yaml"   to "internal:textures/ui/file_text.png",
            "ttf"    to "internal:textures/ui/file_font.png",
            "otf"    to "internal:textures/ui/file_font.png",
            "shader" to "internal:textures/ui/file_code.png",
            "glsl"   to "internal:textures/ui/file_code.png",
            "script" to "internal:textures/ui/file_code.png"
        )

        private val cache = mutableMapOf<String, Int>()

        fun id(path: String) = cache.getOrPut(path) {
            try {
                val tex = Texture.create(path)
                tex.getId()
            } catch (_: Exception) { 0 }
        }

        fun idForExtension(ext: String): Int {
            val p = extMap[ext] ?: FILE_GENERIC
            return id(p)
        }
    }

}