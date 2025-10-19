package kr.jjory.jchatproxy

import com.velocitypowered.api.proxy.ProxyServer
import java.nio.charset.StandardCharsets

class Router(private val server: ProxyServer, private val logger: org.slf4j.Logger, private val cfg: ProxyConfig) {
    fun broadcast(channelId: String, payload: String, kind: String) {
        val allow = when (kind) { "GLOBAL" -> cfg.routingGlobalAllow; "LOCAL" -> cfg.routingLocalAllow; "ADMIN" -> cfg.routingAdminAllow; "PARTY" -> emptyList(); "GUILD" -> emptyList(); else -> emptyList() }
        val data = payload.toByteArray(StandardCharsets.UTF_8)
        val id = com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(channelId)
        if (allow.isEmpty()) { server.allServers.forEach { it.sendPluginMessage(id, data) }; return }
        server.allServers.filter { allow.contains(it.serverInfo.name) }.forEach { it.sendPluginMessage(id, data) }
    }
    fun sendToPlayerServer(channelId: String, usernameOrUuid: String, payload: String): Boolean {
        val data = payload.toByteArray(StandardCharsets.UTF_8); val id = com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(channelId)
        val p = server.allPlayers.firstOrNull { it.username.equals(usernameOrUuid, true) || it.uniqueId.toString().equals(usernameOrUuid, true) }
        p?.currentServer?.ifPresent { it.server.sendPluginMessage(id, data) } ?: return false; return true
    }
}
