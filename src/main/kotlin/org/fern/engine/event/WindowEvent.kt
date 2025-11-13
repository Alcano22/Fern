package org.fern.engine.event

object WindowCloseEvent : Event()

data class WindowResizeEvent(val width: Int, val height: Int) : Event()
data class WindowFramebufferResizeEvent(val width: Int, val height: Int) : Event()

data class WindowKeyPressEvent(val key: Int) : Event()
data class WindowKeyReleaseEvent(val key: Int) : Event()
