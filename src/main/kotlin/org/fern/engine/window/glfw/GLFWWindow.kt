package org.fern.engine.window.glfw

import org.fern.engine.renderer.ClearMask
import org.fern.engine.renderer.Renderer
import org.fern.engine.util.Action
import org.fern.engine.util.Color
import org.fern.engine.util.Time
import org.fern.engine.logger.logger
import org.fern.engine.util.memScoped
import org.fern.engine.util.nullptr
import org.fern.engine.window.Window
import org.fern.engine.window.WindowFramebufferResizeCallback
import org.fern.engine.window.WindowKeyCallback
import org.fern.engine.window.WindowResizeCallback
import org.fern.engine.window.WindowUpdateCallback
import org.lwjgl.glfw.GLFW.*

class GLFWWindow(
    width: Int,
    height: Int,
    private val title: String,
    vSync: Boolean
) : Window {

    private val logger = logger(title)

    private var handle = 0L

    override var vSync = vSync
        set(value) {
            field = value
            if (handle != 0L)
                glfwSwapInterval(if (value) 1 else 0)
        }

    override val clearColor = Color.WHITE

    override var resizeCallback: WindowResizeCallback? = null
        set(value) {
            field = value
            if (handle != nullptr)
                installResizeCallback()
        }

    override var framebufferResizeCallback: WindowFramebufferResizeCallback? = null
        set(value) {
            field = value
            if (handle != nullptr)
                installFramebufferResizeCallback()
        }

    override var keyCallback: WindowKeyCallback? = null
        set(value) {
            field = value
            if (handle != nullptr)
                installKeyCallback()
        }

    override var updateCallback: WindowUpdateCallback? = null
    override var renderCallback: Action? = null

    override val shouldClose get() = glfwWindowShouldClose(handle)

    override val apiType = Window.ApiType.GLFW

    override var width = width
        set(value) { resize(value, height) }
    override var height = height
        set(value) { resize(width, value) }

    override fun init() {
        if (!glfwInit())
            error("Failed to initialize GLFW")
        logger.info { "Initialized GLFW" }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

        handle = glfwCreateWindow(width, height, title, nullptr, nullptr)
        if (handle == nullptr) {
            glfwTerminate()
            error("Failed to create GLFW window")
        }
        logger.info { "Created" }

        val primaryMonitor = glfwGetPrimaryMonitor()
        if (primaryMonitor != nullptr) {
            val (monitorX, monitorY) = memScoped {
                val pX = mallocInt(1)
                val pY = mallocInt(1)
                glfwGetMonitorPos(primaryMonitor, pX, pY)
                pX[0] to pY[0]
            }

            val vidmode = glfwGetVideoMode(primaryMonitor)
            if (vidmode != null) {
                val centerX = monitorX + (vidmode.width() - width) / 2
                val centerY = monitorY + (vidmode.height() - height) / 2
                glfwSetWindowPos(handle, centerX, centerY)
            }
        }

        glfwMakeContextCurrent(handle)

        Renderer.init()

        glfwSwapInterval(if (vSync) 1 else 0)

        if (resizeCallback != null)
            installResizeCallback()
        if (framebufferResizeCallback != null)
            installFramebufferResizeCallback()
        if (keyCallback != null)
            installKeyCallback()
    }

    override fun show() {
        if (handle == nullptr) {
            logger.error { "Failed to show because window is uninitialized" }
            return
        }

        glfwShowWindow(handle)
    }

    override fun hide() {
        if (handle == nullptr) {
            logger.error { "Failed to hide because window is uninitialized" }
            return
        }

        glfwHideWindow(handle)
    }

    private fun installResizeCallback() {
        if (handle == nullptr) {
            logger.error { "Failed to install resize callback because window is uninitialized" }
            return
        }

        glfwSetWindowSizeCallback(handle) { _, w, h ->
            resizeCallback?.invoke(w, h)
        }
    }

    private fun installFramebufferResizeCallback() {
        if (handle == nullptr) {
            logger.error { "Failed to install framebuffer resize callback because window is uninitialized" }
            return
        }

        glfwSetFramebufferSizeCallback(handle) { _, w, h ->
            framebufferResizeCallback?.invoke(w, h)
        }
    }

    private fun installKeyCallback() {
        if (handle == nullptr) {
            logger.error { "Failed to install key callback because window is uninitialized" }
            return
        }

        glfwSetKeyCallback(handle) { _, key, _, action, _ ->
            keyCallback?.invoke(key, action)
        }
    }

    override fun resize(width: Int, height: Int) {
        if (handle == nullptr) {
            logger.error { "Failed to resize because window is uninitialized" }
            return
        }

        this.width = width
        this.height = height
        glfwSetWindowSize(handle, width, height)
    }

    override fun run() {
        if (handle == nullptr) {
            logger.error { "Failed to run because window is uninitialized" }
            return
        }

        Time.reset()
        while (!shouldClose) {
            glfwPollEvents()

            Time.update()

            updateCallback?.invoke(Time.deltaTime)

            Renderer.setClearColor(clearColor)
            Renderer.clear(ClearMask.COLOR, ClearMask.DEPTH)

            renderCallback?.invoke()

            glfwSwapBuffers(handle)
        }
    }

    override fun close() = glfwSetWindowShouldClose(handle, true)

    override fun destroy() {
        if (handle != nullptr) {
            glfwDestroyWindow(handle)
            handle = nullptr
            logger.info { "Destroyed" }
        }
        glfwTerminate()
        logger.info { "Terminated GLFW" }
    }

    override fun getHandle() = handle

}