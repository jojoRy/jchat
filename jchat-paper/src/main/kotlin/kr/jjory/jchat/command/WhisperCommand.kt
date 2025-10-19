package kr.jjory.jchat.command

import kr.jjory.jchat.service.WhisperService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class WhisperCommand(private val whisper: WhisperService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player ?: return true
        if (args.size < 2) { p.sendMessage("§c사용법: /귓 <대상> <메시지>"); return true }
        val targetKey = args[0]
        val content = args.drop(1).joinToString(" ")
        val local = whisper.findLocalTarget(targetKey)
        if (local != null) whisper.sendWhisper(p, local, content) else whisper.sendCrossServer(p, targetKey, content)
        return true
    }
}
