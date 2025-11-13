package org.fern.engine.util

object Time {

    private var lastNano = System.nanoTime()

    var deltaTime = 0f
        private set

    var time = 0f
        private set

    var frameCount = 0L
        private set

    var timeScale = 1f

    var paused = false
        private set

    val fps get() = if (deltaTime > 0f) (1f / deltaTime) else 0f

    fun update() {
        val now = System.nanoTime()
        if (paused) {
            lastNano = now
            deltaTime = 0f
            return
        }

        val rawDelta = (now - lastNano) / 1_000_000_000f
        val scaled = rawDelta * timeScale

        deltaTime = scaled
        time += rawDelta
        frameCount++
        lastNano = now
    }

    fun pause() { paused = true }
    fun resume() { paused = false }

    fun stepFixed(accumulator: Float, fixedStepSeconds: Float, stepCallback: (Float) -> Unit): Float {
        var acc = accumulator + deltaTime
        while (acc >= fixedStepSeconds) {
            stepCallback(fixedStepSeconds)
            acc -= fixedStepSeconds
        }
        return acc
    }

    fun reset() {
        lastNano = System.nanoTime()
        deltaTime = 0f
        time = 0f
        frameCount = 0L
        timeScale = 1f
        paused = false
    }

}