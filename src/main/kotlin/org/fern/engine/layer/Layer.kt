package org.fern.engine.layer

abstract class Layer(val name: String = "Layer") {

    open fun onAttach() {}
    open fun onDetach() {}

    open fun onUpdate(dt: Float) {}

    open fun onRender() {}
    open fun onRenderImGui() {}
}