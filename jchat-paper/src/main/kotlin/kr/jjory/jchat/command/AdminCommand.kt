package kr.jjory.jchat.command

import kr.jjory.jchat.model.ChatMode
import kr.jjory.jchat.service.ChatModeService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdminCommand(private val modes: ChatModeService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player ?: return true
        if (!p.hasPermission("jchat.admin")) { p.sendMessage("§c권한이 없습니다."); return true }
        modes.set(p.uniqueId, ChatMode.ADMIN)
        p.sendMessage("§c[채팅] 관리자 모드로 전환되었습니다.")
        return true
    }
}
