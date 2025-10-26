package kr.jjory.jchat.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class DisabledCommand(private val message: String) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (message.isNotBlank()) {
            sender.sendMessage(message)
        }
        return true
    }
}