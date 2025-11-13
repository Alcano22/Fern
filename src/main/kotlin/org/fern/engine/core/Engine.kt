package org.fern.engine.core

import org.fern.engine.event.*
import org.fern.engine.imgui.ImGuiLayer
import org.fern.engine.layer.EditorLayer
import org.fern.engine.layer.LayerStack
import org.fern.engine.window.Window
import org.lwjgl.glfw.GLFW.*

object Engine {

    val window = Window.create(1280, 720, "Fern Engine", vSync = true)

    private val layerStack = LayerStack()
    private lateinit var imguiLayer: ImGuiLayer

    fun run() {
        window.init()

        window.clearColor.set(0.1f, 0.2f, 0.3f)

        window.resizeCallback = { w, h ->
            if (w > 0 && h > 0)
                EventManager.dispatch(WindowResizeEvent(w, h))
        }

        window.framebufferResizeCallback = { w, h ->
            if (w > 0 && h > 0)
                EventManager.dispatch(WindowFramebufferResizeEvent(w, h))
        }

        window.keyCallback = { key, action ->
            when (action) {
                GLFW_PRESS   -> EventManager.dispatch(WindowKeyPressEvent(key))
                GLFW_RELEASE -> EventManager.dispatch(WindowKeyReleaseEvent(key))
            }
        }

        window.updateCallback = { dt -> layerStack.forEach { it.onUpdate(dt) } }

        window.renderCallback = {
            layerStack.forEach { it.onRender() }

            imguiLayer.begin()
            layerStack.forEach { it.onRenderImGui() }
            imguiLayer.end()
        }

        layerStack.pushLayer(EditorLayer())

        imguiLayer = ImGuiLayer()
        layerStack.pushOverlay(imguiLayer)

        window.show()
        window.run()

        layerStack.dispose()
        window.destroy()
    }

}