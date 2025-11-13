package org.fern.engine.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import org.fusesource.jansi.Ansi

class LevelColorConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val level = event.level
        val padded = level.levelStr.padEnd(5)

        return when (level) {
            Level.TRACE -> Ansi().fgDefault().a(padded).reset().toString()
            Level.DEBUG -> Ansi().fgGreen().a(padded).reset().toString()
            Level.INFO  -> Ansi().fgBlue().a(padded).reset().toString()
            Level.WARN  -> Ansi().fgYellow().a(padded).reset().toString()
            Level.ERROR -> Ansi().fgRed().bold().a(padded).reset().toString()
            else        -> padded
        }
    }
}
