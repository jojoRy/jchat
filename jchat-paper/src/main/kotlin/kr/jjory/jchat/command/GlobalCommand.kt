package kr.jjory.jchat.command

import kr.jjory.jchat.model.ChatMode
import kr.jjory.jchat.service.ChatModeService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GlobalCommand(private val modes: ChatModeService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player ?: return true
        modes.set(p.uniqueId, ChatMode.GLOBAL)
        p.sendMessage("§a[채팅] 전체 모드로 전환되었습니다.")
        return true
    }
}
