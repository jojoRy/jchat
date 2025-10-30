package kr.jjory.jchat.listener

import io.papermc.paper.event.player.AsyncChatEvent
import kr.jjory.jchat.common.Payloads
import kr.jjory.jchat.model.ChatMode
import kr.jjory.jchat.model.ChatMode.*
import kr.jjory.jchat.service.*
import kr.jjory.jchat.common.ColorCodeFormatter
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatListener(private val plugin: org.bukkit.plugin.Plugin, private val config: ConfigService, private val modes: ChatModeService, private val global: GlobalMessenger, private val logger: MessageLogManager, private val prefix: PrefixResolver) : Listener {
    private val mini = MiniMessage.miniMessage()
    private val plain = PlainTextComponentSerializer.plainText()
    init {
        global.initHandlers { payload ->
            try {
                val parts = payload.split("|", limit = 7)
                when (parts[0]) {
                    "GLOBAL" -> {
                        val origin = parts.getOrNull(1) ?: return@initHandlers
                        if (origin.equals(config.serverId, true)) return@initHandlers
                        val display = parts.getOrNull(3) ?: return@initHandlers
                        val msg = parts.getOrNull(4) ?: return@initHandlers
                        val fmt = config.fmtGlobal.replace("{display}", display).replace("{prefix}", "").replace("{message}", msg)
                        Bukkit.getOnlinePlayers().forEach { it.sendMessage(mini.deserialize(fmt)) }
                    }
                    "LOCAL" -> { /* 프록시 단계에선 브로드캐스트만; Paper는 이미 처리함 */ }
                    "ADMIN" -> {
                        val origin = parts.getOrNull(1) ?: return@initHandlers
                        if (origin.equals(config.serverId, true)) return@initHandlers
                        val fromDisplay = parts.getOrNull(3) ?: return@initHandlers
                        val msg = parts.getOrNull(4) ?: return@initHandlers
                        val fmt = config.fmtAdmin
                            .replace("{display}", fromDisplay)
                            .replace("{prefix}", "")
                            .replace("{message}", msg)
                        Bukkit.getOnlinePlayers()
                            .filter { it.hasPermission("jchat.admin") }
                            .forEach { it.sendMessage(mini.deserialize(fmt)) }
                    }
                    "WHISPER" -> {
                        val origin = parts.getOrNull(1) ?: return@initHandlers
                        if (origin.equals(config.serverId, true)) return@initHandlers
                        val senderName = parts.getOrNull(2) ?: return@initHandlers
                        val targetUuidOrName = parts.getOrNull(3) ?: return@initHandlers
                        val msg = parts.getOrNull(4) ?: return@initHandlers
                        val target = Bukkit.getPlayerExact(targetUuidOrName)
                            ?: runCatching { java.util.UUID.fromString(targetUuidOrName) }.getOrNull()?.let { Bukkit.getPlayer(it) }
                            ?: return@initHandlers
                        val recvFmt = config.fmtWhisperReceive.replace("{sender}", senderName).replace("{message}", msg)
                        target.sendMessage(mini.deserialize(recvFmt)); Bukkit.getLogger().info("[WHISPER] $senderName -> ${target.name}: $msg")
                    }
                    "WHISPER_REMOTE" -> {
                        val origin = parts.getOrNull(1) ?: return@initHandlers
                        if (origin.equals(config.serverId, true)) return@initHandlers
                        val senderName = parts.getOrNull(3) ?: return@initHandlers
                        val targetKey = parts.getOrNull(4) ?: return@initHandlers
                        val msg = parts.getOrNull(5) ?: return@initHandlers
                        val target = Bukkit.getOnlinePlayers().firstOrNull {
                            it.name.equals(targetKey, true) ||
                                    plain.serialize(it.displayName()).equals(targetKey, true) ||
                                    it.uniqueId.toString().equals(targetKey, true)
                        } ?: return@initHandlers
                        val recvFmt = config.fmtWhisperReceive.replace("{sender}", senderName).replace("{message}", msg)
                        target.sendMessage(mini.deserialize(recvFmt)); Bukkit.getLogger().info("[WHISPER] $senderName -> ${target.name}: $msg")
                    }
                    "MODE" -> {
                        val uuid = runCatching { java.util.UUID.fromString(parts.getOrNull(2) ?: return@initHandlers) }.getOrNull() ?: return@initHandlers
                        val mode = runCatching { ChatMode.valueOf(parts.getOrNull(3) ?: return@initHandlers) }.getOrNull() ?: ChatMode.GLOBAL
                        modes.applyRemote(uuid, mode)
                        Bukkit.getLogger().info("[MODE] $uuid -> ${mode.name}")
                    }
                    "ANNOUNCE" -> {
                        val msg = parts.getOrNull(1) ?: return@initHandlers
                        val fmt = config.fmtAnnounce.replace("{message}", msg)
                        Bukkit.getOnlinePlayers().forEach { it.sendMessage(mini.deserialize(fmt)) }
                        Bukkit.getLogger().info("[ANNOUNCE] $msg")
                    }
                }
            } catch (_: Throwable) { logger.log("xserver-recv(parse-fail): $payload") }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onChat(e: AsyncChatEvent) {
        if (e.isCancelled) return
        val p = e.player; val mode = modes.get(p.uniqueId)
        var raw = plain.serialize(e.message())
        raw = try { PlaceholderAPI.setPlaceholders(p, raw) } catch (_: Throwable) { raw } // ✅ PAPI 자동 파싱
        val processed = ColorCodeFormatter.apply(raw, p.isOp)
        e.viewers().clear(); e.isCancelled = true
        when (mode) {
            GLOBAL -> {
                val fmt = config.fmtLocal.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", processed)
                Bukkit.getOnlinePlayers().forEach { it.sendMessage(mini.deserialize(fmt)) }
                global.send(Payloads.global(config.serverId, p.name, plain.serialize(p.displayName()), processed))
            }
            LOCAL -> {
                val fmt = config.fmtLocal.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", raw)
                val dist = config.localDistance; val loc = p.location
                p.world.players.filter { it.location.world == loc.world && it.location.distance(loc) <= dist }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                logger.log("local: ${p.name}: $processed"); Bukkit.getLogger().info("[LOCAL] ${plain.serialize(p.displayName())}: $processed")
            }
            ADMIN -> {
                val fmt = config.fmtAdmin.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", processed)
                Bukkit.getOnlinePlayers().filter { it.hasPermission("jchat.admin") }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                global.send(Payloads.admin(config.serverId, p.name, plain.serialize(p.displayName()), processed))
            }
        }
    }
}
