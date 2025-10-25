package kr.jjory.jchat.service

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class WhisperService(private val config: ConfigService, private val global: GlobalMessenger, private val logger: MessageLogManager, private val prefix: PrefixResolver, private val registry: PlayerRegistry) {
    private val lastReply = HashMap<UUID, UUID>()
    private val mini = MiniMessage.miniMessage()
    private val plain = PlainTextComponentSerializer.plainText()
    fun setLast(sender: UUID, target: UUID) { lastReply[sender] = target }
    fun getLast(sender: UUID): UUID? = lastReply[sender]
    fun findLocalTarget(key: String): Player? {
        Bukkit.getPlayerExact(key)?.let { return it }
        val uuid = registry.findUuid(key) ?: return null
        return Bukkit.getPlayer(uuid)
    }
    fun sendWhisper(from: Player, to: Player, contentRaw: String) {
        if (from.uniqueId == to.uniqueId) { from.sendMessage("§c자기 자신에게는 귓속말을 보낼 수 없습니다."); return }
        val content = try { PlaceholderAPI.setPlaceholders(from, contentRaw) } catch (_: Throwable) { contentRaw }
        val sendFmt = config.fmtWhisperSend.replace("{target}", to.name).replace("{message}", content)
        val recvFmt = config.fmtWhisperReceive.replace("{sender}", from.name).replace("{message}", content)
        from.sendMessage(mini.deserialize(sendFmt)); to.sendMessage(mini.deserialize(recvFmt))
        setLast(from.uniqueId, to.uniqueId); setLast(to.uniqueId, from.uniqueId)
        global.send(kr.jjory.jchat.common.Payloads.whisper(config.serverId, from.name, to.uniqueId.toString(), content))
    }
    fun sendCrossServer(from: Player, targetKey: String, contentRaw: String) {
        val pd = plain.serialize(from.displayName())
        if (from.name.equals(targetKey, true) || pd.equals(targetKey, true) || from.uniqueId.toString().equals(targetKey, true)) {
            from.sendMessage("§c자기 자신에게는 귓속말을 보낼 수 없습니다."); return
        }
        val content = try { PlaceholderAPI.setPlaceholders(from, contentRaw) } catch (_: Throwable) { contentRaw }
        global.send(kr.jjory.jchat.common.Payloads.whisperRemote(config.serverId, from.uniqueId.toString(), from.name, targetKey, content))
        from.sendMessage("§d[귓속말] §7(다른 서버의 §f$targetKey§7 에게 보냈습니다) §f$content")
    }
}
