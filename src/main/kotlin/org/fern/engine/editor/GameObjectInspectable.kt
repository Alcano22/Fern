package org.fern.engine.editor

import imgui.ImGui
import org.fern.engine.imgui.ImGuiEx
import org.fern.engine.logger.logger
import org.fern.engine.scene.GameObject
import org.fern.engine.scene.component.Component
import org.fern.engine.scene.component.ComponentRegistry
import org.fern.engine.scene.component.Transform
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class GameObjectInspectable(
    private val go: GameObject
) : Inspectable {

    companion object {
        private val logger = logger()
    }

    override val title get() = go.name

    override fun draw() {
        ImGuiEx.checkbox("##Active", go::active)
        ImGui.sameLine()
        ImGuiEx.inputText("##Name", go::name)

        ImGui.separator()

        go.transform.let { t ->
            ImGui.pushID("Transform")
            ImGuiEx.disabled { ImGui.checkbox("##Enabled", true) }

            val enabledWidth = ImGui.getItemRectSizeX() + ImGui.getStyle().itemInnerSpacingX

            ImGui.sameLine()
            ImGuiEx.collapsingHeader("Transform") {
                ImGuiEx.indented(enabledWidth) {
                    ImGuiEx.dragFloat2("Position", t.position, speed = 0.1f)
                    ImGuiEx.dragFloat("Rotation", t::rotation, speed = 1f)
                    ImGuiEx.dragFloat2Linked("Scale", t.scale, speed = 0.1f)

                    ImGuiEx.spacing(3)
                }
            }
            ImGui.popID()
        }

        val components = go.getAll<Component>()
        loop@ for (comp in components) {
            if (comp is Transform) continue

            ImGui.pushID(comp.hashCode())
            val title = comp::class.simpleName ?: "Component"

            ImGuiEx.checkbox("##Enabled", comp::enabled)
            val enabledWidth = ImGui.getItemRectSizeX() + ImGui.getStyle().itemInnerSpacingX

            ImGui.sameLine()
            ImGuiEx.collapsingHeader(title) {
                ImGuiEx.indented(enabledWidth) {
                    ReflectionInspector.inspectObject(comp)

                    ImGui.separator()
                    ImGuiEx.button("Remove Component") {
                        go.remove(comp)
                        ImGui.popID()
                        break@loop
                    }

                    ImGuiEx.spacing(3)
                }
            }

            ImGui.popID()
        }

        ImGuiEx.spacing(5)

        ImGuiEx.button("Add Component") {
            ImGui.openPopup("add_component_popup")
        }
        renderAddComponentPopup()
    }

    private fun renderAddComponentPopup() {
        if (ImGui.beginPopup("add_component_popup")) {
            for ((category, items) in ComponentRegistry.categories()) {
                if (ImGui.beginMenu(category)) {
                    for (info in items) {
                        val alreadyHas = if (info.isSingle)
                            go.getAll<Component>().any { info.cls.isInstance(it) }
                        else
                            false

                        val enabled = !alreadyHas
                        if (ImGui.menuItem(info.name, "", false, enabled)) {
                            val instance = runCatching {
                                info.cls.java.getDeclaredConstructor().newInstance()
                            }.onFailure {
                                logger.error(it) { "Failed to instantiate: ${info.cls.qualifiedName}" }
                            }.getOrNull() ?: continue

                            go.add(instance)
                        }
                    }
                    ImGui.endMenu()
                }
            }
            ImGui.endPopup()
        }
    }

}