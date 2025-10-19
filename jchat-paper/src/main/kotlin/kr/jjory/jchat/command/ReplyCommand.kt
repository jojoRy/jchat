package kr.jjory.jchat.command

import kr.jjory.jchat.service.WhisperService
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReplyCommand(private val whisper: WhisperService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player ?: return true
        val last = whisper.getLast(p.uniqueId) ?: run { p.sendMessage("§c답장할 대상이 없습니다."); return true }
        val to = Bukkit.getPlayer(last) ?: run { p.sendMessage("§c대상을 찾을 수 없습니다."); return true }
        if (args.isEmpty()) { p.sendMessage("§c사용법: /답장 <메시지>"); return true }
        whisper.sendWhisper(p, to, args.joinToString(" "))
        return true
    }
}
