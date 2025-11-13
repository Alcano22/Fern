package org.fern.engine.scene.component

import org.fern.engine.logger.logger
import org.fern.engine.script.api.ScriptContext
import org.fern.engine.script.api.ScriptLifecycle
import org.fern.engine.script.kts.ScriptingHost
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

data class ScriptRef(val path: String)

@RegisterComponent("Script")
@SingleComponent
class ScriptComponent : Component() {

    companion object {
        val logger = logger()
    }

    var script: ScriptRef? = null
        set(value) {
            field = value
            if (started)
                reload()
        }

    private lateinit var host: ScriptingHost

    private var ctx: ScriptContext? = null
    private var instance: ScriptLifecycle? = null

    override fun onAdded() {
        ctx = ScriptContext(gameObject)
        host = ScriptingHost(buildApiClasspath())
    }

    override fun start() {
        reload()
        instance?.onStart()
    }

    override fun update(dt: Float) {
        instance?.onUpdate(dt)
    }

    override fun onRemoved() {
        runCatching { instance?.onDestroy() }
        instance = null
        ctx = null
    }

    private fun reload() {
        instance = null
        val ref = script ?: return
        val file = File("assets/${ref.path}")
        instance = host.load(file, ctx!!)
        if (instance != null)
            logger.info { "Script ready: ${ref.path}" }
        else
            logger.warn { "Script load failed: ${ref.path}" }
    }

    private fun buildApiClasspath(): List<File> {
        fun loc(cls: Class<*>) = cls.protectionDomain.codeSource?.location?.toURI()?.let(::File)

        return listOfNotNull(
            loc(ScriptContext::class.java),
            loc(BasicJvmScriptingHost::class.java),
            loc(ScriptCompilationConfiguration::class.java),
            loc(JvmStatic::class.java)
        ).distinct()
    }

}