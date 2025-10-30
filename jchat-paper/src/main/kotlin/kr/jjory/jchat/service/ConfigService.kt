package kr.jjory.jchat.service

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class ConfigService(private val plugin: Plugin) {
    fun reload() = plugin.reloadConfig()
    val serverId: String get() = plugin.config.getString("server-id", "").let { if (it.isNullOrBlank()) Bukkit.getServer().name else it }
    val channel: String get() = plugin.config.getString("plugin-channel", "jchat:main")!!
    fun ensureServerId() {
        val cur = plugin.config.getString("server-id", "")
        if (cur.isNullOrBlank()) { plugin.config.set("server-id", Bukkit.getServer().name); plugin.saveConfig() }
    }
    val localDistance: Double get() = plugin.config.getDouble("chat.local-distance", 100.0)
    val clickable: Boolean get() = plugin.config.getBoolean("chat.enable-clickable-names", true)
    val logEnabled: Boolean get() = plugin.config.getBoolean("chat.log-messages", true)
    val fmtGlobal: String get() = plugin.config.getString("format.global")!!
    val fmtLocal: String get() = plugin.config.getString("format.local")!!
    val fmtAdmin: String get() = plugin.config.getString("format.admin")!!
    val fmtWhisperSend: String get() = plugin.config.getString("format.whisper-send")!!
    val fmtWhisperReceive: String get() = plugin.config.getString("format.whisper-receive")!!
    val fmtAnnounce: String get() = plugin.config.getString("format.announce", "{message}")!!
}
