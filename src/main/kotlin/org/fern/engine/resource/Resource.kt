package org.fern.engine.resource

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

enum class ResourceDomain {
    USER,
    INTERNAL
}

data class Resource(
    val domain: ResourceDomain,
    val path: String,
    val bytes: ByteArray
) : AutoCloseable {

    private val closed = AtomicBoolean(false)

    val size get() = bytes.size
    val isClosed get() = closed.get()

    fun asByteBuffer() = ByteBuffer.wrap(bytes)

    override fun close() {
        if (closed.compareAndSet(false, true))
            bytes.fill(0)
    }

}
