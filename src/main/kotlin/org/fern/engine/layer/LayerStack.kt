package org.fern.engine.layer

import org.fern.engine.logger.logger

class LayerStack : Iterable<Layer> {

    companion object {
        private val logger = logger()
    }

    private val _layers = mutableListOf<Layer>()
    private var layerInsertIndex = 0

    val layers get() = _layers.toList()
    val size get() = _layers.size

    fun pushLayer(layer: Layer) {
        _layers.add(layerInsertIndex, layer)
        layerInsertIndex++
        layer.onAttach()

        logger.info { "Pushed layer: ${layer.name}" }
    }

    fun pushOverlay(overlay: Layer) {
        _layers.add(overlay)
        overlay.onAttach()

        logger.info { "Pushed overlay: ${overlay.name}" }
    }

    fun popLayer(layer: Layer) {
        val end = layerInsertIndex.coerceAtMost(_layers.size)
        val subIndex = _layers.subList(0, end).indexOf(layer)
        if (subIndex == -1) return

        layer.onDetach()
        _layers.removeAt(subIndex)
        layerInsertIndex--

        logger.info { "Popped layer: ${layer.name}" }
    }

    fun popOverlay(overlay: Layer) {
        if (layerInsertIndex >= _layers.size) return

        val sub = _layers.subList(layerInsertIndex, _layers.size)
        val subIndex = sub.indexOf(overlay)
        if (subIndex == -1) return

        val realIndex = layerInsertIndex + subIndex
        overlay.onDetach()
        _layers.removeAt(realIndex)

        logger.info { "Popped overlay: ${overlay.name}" }
    }

    fun clear() {
        _layers.forEach { it.onDetach() }
        _layers.clear()
        layerInsertIndex = 0
    }

    fun dispose() = clear()

    override fun iterator() = _layers.iterator()

}