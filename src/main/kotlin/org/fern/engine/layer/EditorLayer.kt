package org.fern.engine.layer

import org.fern.engine.core.Engine
import org.fern.engine.editor.EditorSystem
import org.fern.engine.editor.FileExplorerWindow
import org.fern.engine.editor.GameObjectInspectable
import org.fern.engine.editor.HierarchyWindow
import org.fern.engine.editor.InspectorWindow
import org.fern.engine.editor.TextureAssetInspectable
import org.fern.engine.editor.ViewportWindow
import org.fern.engine.event.EventManager
import org.fern.engine.event.WindowFramebufferResizeEvent
import org.fern.engine.logger.logger
import org.fern.engine.renderer.*
import org.fern.engine.scene.Scene
import org.fern.engine.scene.component.CameraComponent
import org.fern.engine.scene.component.ScriptComponent
import org.fern.engine.scene.component.SpriteRenderer
import java.io.File

class EditorLayer : Layer("EditorLayer") {

    companion object {
        val logger = logger()
    }

    val scene = Scene()

    private lateinit var framebuffer: Framebuffer

    private lateinit var editor: EditorSystem
    private lateinit var viewportWindow: ViewportWindow

    override fun onAttach() {
        EventManager.addCallback<WindowFramebufferResizeEvent> { (w, h) -> onFramebufferResize(w, h) }

        framebuffer = Framebuffer.create(FramebufferSpecs(1, 1))

        editor = EditorSystem(
            persistenceFile = File("config/editor_state.json"),
            enableDockSpace = true
        )

        viewportWindow = ViewportWindow(framebuffer, 16f / 9f)
        val inspectorWindow = InspectorWindow()
        val hierarchyWindow = HierarchyWindow(scene) { go ->
            if (go == null) return@HierarchyWindow

            val goInspectable = GameObjectInspectable(go)
            inspectorWindow.select(goInspectable)
        }
        val fileExplorerWindow = FileExplorerWindow { path ->
            if (path == null || !path.endsWith(".png")) return@FileExplorerWindow

            val texInspectable = TextureAssetInspectable(path)
            inspectorWindow.select(texInspectable)
        }

        editor.registry.register(viewportWindow)
        editor.registry.register(inspectorWindow)
        editor.registry.register(hierarchyWindow)
        editor.registry.register(fileExplorerWindow)

        editor.init()

        scene.createGameObject("Square").apply {
            add(SpriteRenderer())
            add(ScriptComponent())
        }

        scene.createGameObject("Camera 0").apply {
            add(CameraComponent()).apply { targetDisplay = 0 }
        }

        scene.createGameObject("Camera 1").apply {
            add(CameraComponent()).apply { targetDisplay = 1 }
        }

        scene.startIfNeeded()
    }

    private fun onFramebufferResize(width: Int, height: Int) {
        framebuffer.resize(width, height)
        val activeCam = findActiveCameraForDisplay(viewportWindow.targetDisplay)
        if (activeCam != null) {
            val aspect = width.toFloat() / height.toFloat()
            activeCam.camera.aspectRatio = aspect
        }
    }

    override fun onDetach() {
        scene.dispose()
        framebuffer.dispose()
        editor.shutdown()
    }

    override fun onUpdate(dt: Float) {
        scene.update(dt)
        editor.update(dt)
    }

    override fun onRender() {
        framebuffer.bind()
        Renderer.setViewport(0, 0, framebuffer.width, framebuffer.height)
        Renderer.clear(ClearMask.COLOR, ClearMask.DEPTH)

        val activeCam = findActiveCameraForDisplay(viewportWindow.targetDisplay)
        if (activeCam == null) {
            viewportWindow.overlayMessage = "No Camera found"
            Framebuffer.unbind()
            Renderer.setViewport(0, 0, Engine.window.width, Engine.window.height)
            return
        }

        viewportWindow.overlayMessage = null

        val aspect = framebuffer.width.toFloat() / framebuffer.height.toFloat()
        val camera = activeCam.camera
        camera.aspectRatio = aspect

        Renderer.begin(camera)
        scene.render()
        Renderer.end()

        Framebuffer.unbind()
        Renderer.setViewport(0, 0, Engine.window.width, Engine.window.height)
    }

    override fun onRenderImGui() {
        editor.render()
    }

    private fun findActiveCameraForDisplay(display: Int) = scene
        .allGameObjectsSequence()
        .filter { it.active }
        .mapNotNull { it.get<CameraComponent>() }
        .firstOrNull { it.enabled && it.targetDisplay == display }

}