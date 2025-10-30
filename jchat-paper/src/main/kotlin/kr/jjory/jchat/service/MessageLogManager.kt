package kr.jjory.jchat.service

import org.bukkit.plugin.Plugin
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MessageLogManager(private val plugin: Plugin) {
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")
    fun log(line: String) { val folder = File(plugin.dataFolder, "logs").apply { mkdirs() }; val file = File(folder, dateFmt.format(LocalDate.now()) + ".yml"); val ts = timeFmt.format(LocalDateTime.now()); file.appendText("- [$ts] " + line + "\n") }
}