import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"

    application
}

group = "org.fern"
version = "1.0.0"

val os = OperatingSystem.current()!!

val lwjglVersion         = "3.3.6"
val jomlVersion          = "1.10.8"
val imguiVersion         = "1.90.0"
val fastutilVersion      = "8.5.9"
val classgraphVersion    = "4.8.154"
val kotlinLoggingVersion = "3.0.5"
val logbackVersion       = "1.4.11"
val jansiVersion         = "2.4.0"

val lwjglNatives = when {
    os.isWindows -> "natives-windows"
    os.isLinux   -> "natives-linux"
    os.isMacOsX  -> "natives-macos"
    else -> error("Unsupported OS: $os")
}

val imguiNatives = when {
    os.isWindows -> "imgui-java-natives-windows"
    os.isLinux   -> "imgui-java-natives-linux"
    os.isMacOsX  -> "imgui-java-natives-macos"
    else -> error("Unsupported OS: $os")
}

val lwjglArtifacts = listOf(
    "lwjgl",
    "lwjgl-glfw",
    "lwjgl-opengl",
    "lwjgl-stb"
)

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    lwjglArtifacts.forEach {
        implementation("org.lwjgl:$it")
        runtimeOnly("org.lwjgl:$it::$lwjglNatives")
    }

    implementation("org.joml:joml:$jomlVersion")

    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    runtimeOnly("io.github.spair:$imguiNatives:$imguiVersion")

    implementation("it.unimi.dsi:fastutil:$fastutilVersion")

    implementation("io.github.classgraph:classgraph:$classgraphVersion")

    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.fusesource.jansi:jansi:$jansiVersion")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.fern.engine.core.AppKt")

    applicationDefaultJvmArgs = listOf(
        "-DrenderApi=OPEN_GL",
        "-DwindowApi=GLFW",
        "-DLOG_LEVEL=DEBUG"
    )
}
