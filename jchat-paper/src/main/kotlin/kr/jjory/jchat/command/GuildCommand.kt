package kr.jjory.jchat.command

import kr.jjory.jchat.model.ChatMode
import kr.jjory.jchat.service.ChatModeService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildCommand(private val modes: ChatModeService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player ?: return true
        modes.set(p.uniqueId, ChatMode.GUILD)
        p.sendMessage("§6[채팅] 길드 모드로 전환되었습니다.")
        return true
    }
}
