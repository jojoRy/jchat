package kr.jjory.jchat.command

import kr.jjory.jchat.service.ConfigService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

class ReloadCommand(private val plugin: Plugin, private val config: ConfigService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("jchat.admin")) { sender.sendMessage("§c권한이 없습니다."); return true }
        config.reload()
        sender.sendMessage("§aJChat 설정이 새로 고침되었습니다.")
        return true
    }
}
