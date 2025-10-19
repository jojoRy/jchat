package kr.jjory.jchat.listener

import io.papermc.paper.event.player.AsyncChatEvent
import kr.jjory.jchat.model.ChatMode
import kr.jjory.jchat.service.*
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatListener(private val plugin: org.bukkit.plugin.Plugin, private val config: ConfigService, private val modes: ChatModeService, private val global: GlobalMessenger, private val logger: MessageLogManager, private val prefix: PrefixResolver, private val partyGuild: PartyGuildService) : Listener {
    private val mini = MiniMessage.miniMessage()
    private val plain = PlainTextComponentSerializer.plainText()
    init {
        global.initHandlers { payload ->
            try {
                val parts = payload.split("|", limit = 7)
                when (parts[0]) {
                    "GLOBAL" -> {
                        val display = parts[3]; val msg = parts[4]
                        val fmt = config.fmtGlobal.replace("{display}", display).replace("{prefix}", "").replace("{message}", msg)
                        Bukkit.getOnlinePlayers().forEach { it.sendMessage(mini.deserialize(fmt)) }
                        Bukkit.getLogger().info("[GLOBAL] $display: $msg")
                    }
                    "LOCAL" -> { /* 프록시 단계에선 브로드캐스트만; Paper는 이미 처리함 */ }
                    "ADMIN" -> { /* 동일 */ }
                    "WHISPER" -> {
                        val senderName = parts[2]; val targetUuidOrName = parts[3]; val msg = parts[4]
                        val target = Bukkit.getPlayerExact(targetUuidOrName) ?: runCatching { java.util.UUID.fromString(targetUuidOrName) }.getOrNull()?.let { Bukkit.getPlayer(it) } ?: return@initHandlers
                        val recvFmt = config.fmtWhisperReceive.replace("{sender}", senderName).replace("{message}", msg)
                        target.sendMessage(mini.deserialize(recvFmt)); Bukkit.getLogger().info("[WHISPER] $senderName -> ${target.name}: $msg")
                    }
                    "WHISPER_REMOTE" -> {
                        val senderName = parts[3]; val targetKey = parts[4]; val msg = parts[5]
                        val target = Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(targetKey, true) || plain.serialize(it.displayName()).equals(targetKey, true) || it.uniqueId.toString().equals(targetKey, true) } ?: return@initHandlers
                        val recvFmt = config.fmtWhisperReceive.replace("{sender}", senderName).replace("{message}", msg)
                        target.sendMessage(mini.deserialize(recvFmt)); Bukkit.getLogger().info("[WHISPER] $senderName -> ${target.name}: $msg")
                    }
                    "MODE" -> { Bukkit.getLogger().info("[MODE] ${parts[2]} -> ${parts[3]}") }
                    "ANNOUNCE" -> {
                        val msg = parts[1]; val fmt = config.fmtAnnounce.replace("{message}", msg)
                        Bukkit.getOnlinePlayers().forEach { it.sendMessage(mini.deserialize(fmt)) }
                        Bukkit.getLogger().info("[ANNOUNCE] $msg")
                    }
                    "PARTY" -> {
                        val partyKey = parts[2]; val fromDisplay = parts[4]; val msg = parts[5]
                        val fmt = config.fmtParty.replace("{display}", fromDisplay).replace("{prefix}", "").replace("{message}", msg)
                        Bukkit.getOnlinePlayers().filter { p -> partyGuild.partyKey(p)?.equals(partyKey, true) == true }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                    }
                    "GUILD" -> {
                        val guildKey = parts[2]; val fromDisplay = parts[4]; val msg = parts[5]
                        val fmt = config.fmtGuild.replace("{display}", fromDisplay).replace("{prefix}", "").replace("{message}", msg)
                        Bukkit.getOnlinePlayers().filter { p -> partyGuild.guildKey(p)?.equals(guildKey, true) == true }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                    }
                }
            } catch (_: Throwable) { logger.log("xserver-recv(parse-fail): $payload") }
            logger.log("xserver-recv: $payload")
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onChat(e: AsyncChatEvent) {
        if (e.isCancelled) return
        val p = e.player; val mode = modes.get(p.uniqueId)
        var raw = plain.serialize(e.message())
        raw = try { PlaceholderAPI.setPlaceholders(p, raw) } catch (_: Throwable) { raw } // ✅ PAPI 자동 파싱
        e.viewers().clear(); e.isCancelled = true
        when (mode) {
            kr.jjory.jchat.model.ChatMode.GLOBAL -> {
                val fmt = config.fmtGlobal.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", raw)
                Bukkit.getOnlinePlayers().forEach { it.sendMessage(mini.deserialize(fmt)) }
                global.send(kr.jjory.jchat.common.Payloads.global(config.serverId, p.name, plain.serialize(p.displayName()), raw))
                logger.log("global: ${p.name}: $raw")
            }
            kr.jjory.jchat.model.ChatMode.LOCAL -> {
                val fmt = config.fmtLocal.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", raw)
                val dist = config.localDistance; val loc = p.location
                p.world.players.filter { it.location.world == loc.world && it.location.distance(loc) <= dist }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                logger.log("local: ${p.name}: $raw"); Bukkit.getLogger().info("[LOCAL] ${plain.serialize(p.displayName())}: $raw")
            }
            kr.jjory.jchat.model.ChatMode.ADMIN -> {
                val fmt = config.fmtAdmin.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", raw)
                Bukkit.getOnlinePlayers().filter { it.hasPermission("jchat.admin") }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                logger.log("admin: ${p.name}: $raw"); Bukkit.getLogger().info("[ADMIN] ${plain.serialize(p.displayName())}: $raw")
            }
            kr.jjory.jchat.model.ChatMode.PARTY -> {
                val key = partyGuild.partyKey(p)
                if (key == null) { p.sendMessage("§c[파티] 파티에 속해있지 않습니다."); return }
                val fmt = config.fmtParty.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", raw)
                Bukkit.getOnlinePlayers().filter { pp -> partyGuild.partyKey(pp)?.equals(key, true) == true }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                global.send(kr.jjory.jchat.common.Payloads.party(config.serverId, key, p.name, plain.serialize(p.displayName()), raw))
                logger.log("party: ${p.name}@$key: $raw")
            }
            kr.jjory.jchat.model.ChatMode.GUILD -> {
                val key = partyGuild.guildKey(p)
                if (key == null) { p.sendMessage("§c[길드] 길드에 속해있지 않습니다."); return }
                val fmt = config.fmtGuild.replace("{display}", plain.serialize(p.displayName())).replace("{prefix}", prefix.prefixOf(p)).replace("{message}", raw)
                Bukkit.getOnlinePlayers().filter { pp -> partyGuild.guildKey(pp)?.equals(key, true) == true }.forEach { it.sendMessage(mini.deserialize(fmt)) }
                global.send(kr.jjory.jchat.common.Payloads.guild(config.serverId, key, p.name, plain.serialize(p.displayName()), raw))
                logger.log("guild: ${p.name}@$key: $raw")
            }
        }
    }
}
