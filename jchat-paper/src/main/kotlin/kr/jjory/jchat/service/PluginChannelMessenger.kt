package kr.jjory.jchat.service

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.messaging.PluginMessageListener

class PluginChannelMessenger(private val plugin: Plugin, private val config: ConfigService, private val logger: MessageLogManager) {
    private var incomingHandler: ((String) -> Unit)? = null
    fun registerIncoming(handler: (String) -> Unit) {
        incomingHandler = handler
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, config.channel)
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, config.channel, PluginMessageListener { _, _, message ->
            try { incomingHandler?.invoke(String(message, Charsets.UTF_8)) } catch (t: Throwable) { Bukkit.getLogger().warning("[JChat] Incoming channel parse fail: ${t.message}") }
        })
    }
    fun send(payload: String): Boolean {
        val data = payload.toByteArray(Charsets.UTF_8)
        val carrier: Player = Bukkit.getOnlinePlayers().firstOrNull() ?: return false
        carrier.sendPluginMessage(plugin, config.channel, data)
        logger.log("proxy-send: $payload")
        return true
    }
}
