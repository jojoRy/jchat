package kr.jjory.jchat.command

import kr.jjory.jchat.service.ConfigService
import kr.jjory.jchat.service.GlobalMessenger
import kr.jjory.jchat.common.AnnounceFormatter
import kr.jjory.jchat.common.Payloads
import kr.jjory.jchat.common.ColorCodeFormatter
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AnnounceCommand(private val cfg: ConfigService, private val net: GlobalMessenger) : CommandExecutor {
    private val mini = MiniMessage.miniMessage()
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("jchat.admin")) { sender.sendMessage("§c권한이 없습니다."); return true }
        if (args.isEmpty()) { sender.sendMessage("§c사용법: /공지 <메시지>"); return true }
        val raw = args.joinToString(" ")
        val parsed = when (sender) {
            is Player -> try { PlaceholderAPI.setPlaceholders(sender, raw) } catch (_: Throwable) { raw }
            else -> raw
        }
        val allowColors = sender !is Player || sender.isOp
        val processed = ColorCodeFormatter.apply(parsed, allowColors)
        val padded = AnnounceFormatter.surroundWithBlankLines(processed)
        val formatted = cfg.fmtAnnounce.replace("{message}", padded)
        val delivered = net.send(Payloads.announce(padded))
        if (!delivered) {
            Bukkit.getOnlinePlayers().forEach { it.sendMessage(mini.deserialize(formatted)) }
        }
        return true
    }
}
