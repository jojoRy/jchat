package kr.jjory.jchatproxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import java.nio.charset.StandardCharsets
import java.util.*

class PluginMessaging @Inject constructor() {
    private lateinit var server: ProxyServer
    private lateinit var logger: org.slf4j.Logger
    private lateinit var cfg: ProxyConfig
    private lateinit var store: StateStore
    private lateinit var router: Router
    private lateinit var moderation: Moderation
    private lateinit var container: PluginContainer
    private lateinit var id: ChannelIdentifier

    fun init(server: ProxyServer, logger: org.slf4j.Logger, cfg: ProxyConfig, store: StateStore, router: Router, moderation: Moderation, container: PluginContainer) {
        this.server = server; this.logger = logger; this.cfg = cfg; this.store = store; this.router = router; this.moderation = moderation; this.container = container
        this.id = MinecraftChannelIdentifier.from(cfg.channel)
    }

    fun register() { server.channelRegistrar.register(id); server.eventManager.register(container, this) }

    @Subscribe fun onFirstServer(e: PlayerChooseInitialServerEvent) {}
    @Subscribe fun onConnected(e: ServerConnectedEvent) {}
    @Subscribe fun onDisconnect(e: DisconnectEvent) { store.unindex(e.player.uniqueId) }

    @Subscribe fun onPluginMessage(e: PluginMessageEvent) {
        if (e.identifier != id) return
        val raw = String(e.data, StandardCharsets.UTF_8); val parts = raw.split("|")
        when (parts[0]) {
            "GLOBAL" -> {
                val serverId = parts[1]; val fromName = parts[2]; val fromDisplay = parts[3]; val msg = parts[4]
                if (moderation.isMuted(fromName)) return
                val filtered = moderation.filter(msg) ?: return
                if (cfg.mirrorGlobal) logger.info("[GLOBAL] $fromDisplay: $filtered")
                router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.global(serverId, fromName, fromDisplay, filtered), "GLOBAL")
            }
            "ADMIN" -> {
                val serverId = parts[1]; val fromName = parts[2]; val fromDisplay = parts[3]; val msg = parts[4]
                if (moderation.isMuted(fromName)) return
                val filtered = moderation.filter(msg) ?: return
                if (cfg.mirrorAdmin) logger.info("[ADMIN] $fromDisplay: $filtered")
                router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.admin(serverId, fromName, fromDisplay, filtered), "ADMIN")
            }
            "WHISPER" -> {
                val serverId = parts[1]; val fromName = parts[2]; val targetKey = parts[3]; val msg = parts[4]
                if (moderation.isMuted(fromName)) return
                val filtered = moderation.filter(msg) ?: return
                if (cfg.mirrorWhisper) logger.info("[WHISPER] $fromName -> $targetKey: $filtered")
                val ok = router.sendToPlayerServer(cfg.channel, targetKey, kr.jjory.jchat.common.Payloads.whisper(serverId, fromName, targetKey, filtered))
                if (!ok) { router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.whisper(serverId, fromName, targetKey, filtered), "GLOBAL") }
            }
            "WHISPER_REMOTE" -> {
                val serverId = parts[1]; val fromUuid = parts[2]; val fromName = parts[3]; val targetKey = parts[4]; val msg = parts[5]
                if (moderation.isMuted(fromName)) return
                val filtered = moderation.filter(msg) ?: return
                if (cfg.mirrorWhisper) logger.info("[WHISPER] $fromName -> $targetKey: $filtered")
                val ok = router.sendToPlayerServer(cfg.channel, targetKey, kr.jjory.jchat.common.Payloads.whisperRemote(serverId, fromUuid, fromName, targetKey, filtered))
                if (!ok) { router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.whisperRemote(serverId, fromUuid, fromName, targetKey, filtered), "GLOBAL") }
            }
            "MODE" -> {
                val uuid = UUID.fromString(parts[2]); val mode = parts[3]; val cm = try { ChatMode.valueOf(mode) } catch (_: Throwable) { ChatMode.GLOBAL }
                store.setMode(uuid, cm)
                router.broadcast(cfg.channel, raw, "MODE")
                if (cfg.mirrorAdmin) logger.info("[MODE] $uuid -> $mode")
            }
            "INDEX" -> {
                val uuid = UUID.fromString(parts[2]); val name = parts[3]; val display = parts[4]
                store.index(uuid, name, display); logger.info("[INDEX] $name/$display ($uuid)")
            }
            "ANNOUNCE" -> {
                val msg = parts[1]; val filtered = moderation.filter(msg) ?: return
                if (cfg.mirrorAnnounce) logger.info("[ANNOUNCE] $filtered")
                router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.announce(filtered), "GLOBAL")
            }
            "PARTY" -> {
                val partyKey = parts[2]; val fromName = parts[3]; val fromDisplay = parts[4]; val msg = parts[5]
                val filtered = moderation.filter(msg) ?: return
                if (cfg.mirrorParty) logger.info("[PARTY:$partyKey] $fromDisplay: $filtered")
                router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.party(parts[1], partyKey, fromName, fromDisplay, filtered), "PARTY")
            }
            "GUILD" -> {
                val guildKey = parts[2]; val fromName = parts[3]; val fromDisplay = parts[4]; val msg = parts[5]
                val filtered = moderation.filter(msg) ?: return
                if (cfg.mirrorGuild) logger.info("[GUILD:$guildKey] $fromDisplay: $filtered")
                router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.guild(parts[1], guildKey, fromName, fromDisplay, filtered), "GUILD")
            }
        }
    }
}
