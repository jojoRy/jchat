package kr.jjory.jchat.service

import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*

class PlayerDataService(private val plugin: Plugin) {
    private fun fileOf(uuid: UUID) = File(plugin.dataFolder, "playerdata/${uuid}.yml").apply { parentFile.mkdirs() }
    fun saveMode(uuid: UUID, mode: String) { fileOf(uuid).writeText("mode: $mode\n") }
    fun loadMode(uuid: UUID): String? { val f = fileOf(uuid); if (!f.exists()) return null; return f.readLines().firstOrNull()?.substringAfter("mode: ")?.trim() }
}
