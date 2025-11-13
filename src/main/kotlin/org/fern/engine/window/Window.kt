package org.fern.engine.window

import org.fern.engine.util.Action
import org.fern.engine.util.Color
import org.fern.engine.window.glfw.GLFWWindow

typealias WindowResizeCallback = (Int, Int) -> Unit
typealias WindowFramebufferResizeCallback = (Int, Int) -> Unit
typealias WindowKeyCallback = (Int, Int) -> Unit
typealias WindowUpdateCallback = (Float) -> Unit

interface Window {

    enum class ApiType(
        val factory: (Int, Int, String, Boolean) -> Window
    ) {
        GLFW(::GLFWWindow)
    }

    companion object {
        fun create(width: Int, height: Int, title: String, vSync: Boolean = true): Window {
            val apiTypeStr = System.getProperty("windowApi").trim()
            val apiType = ApiType.valueOf(apiTypeStr)
            return apiType.factory(width, height, title, vSync)
        }
    }

    var vSync: Boolean

    val clearColor: Color

    var resizeCallback: WindowResizeCallback?
    var framebufferResizeCallback: WindowFramebufferResizeCallback?
    var keyCallback: WindowKeyCallback?
    var updateCallback: WindowUpdateCallback?
    var renderCallback: Action?

    val shouldClose: Boolean

    val apiType: ApiType

    var width: Int
    var height: Int

    fun init()

    fun show()
    fun hide()

    fun resize(width: Int, height: Int)

    fun run()

    fun close()
    fun destroy()

    fun getHandle(): Long

}
