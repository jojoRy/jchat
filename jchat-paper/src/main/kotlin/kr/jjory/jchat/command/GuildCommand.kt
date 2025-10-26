package kr.jjory.jchat.command

import kr.jjory.jchat.model.ChatMode
import kr.jjory.jchat.service.ChatModeService
import kr.jjory.jchat.service.PartyGuildService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildCommand(private val modes: ChatModeService, private val partyGuild: PartyGuildService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player ?: return true
        if (!partyGuild.isGuildChatAvailable) {
            p.sendMessage("§c[채팅] MMOCore가 설치되어 있지 않아 길드 채팅을 사용할 수 없습니다.")
            return true
        }
        modes.set(p.uniqueId, ChatMode.GUILD)
        p.sendMessage("§6[채팅] 길드 모드로 전환되었습니다.")
        return true
    }
}
