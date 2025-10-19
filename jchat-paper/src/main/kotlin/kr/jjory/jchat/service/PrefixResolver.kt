package kr.jjory.jchat.service

import org.bukkit.entity.Player
import net.luckperms.api.LuckPermsProvider

class PrefixResolver {
    fun prefixOf(player: Player): String {
        return try {
            val api = LuckPermsProvider.get()
            val user = api.userManager.getUser(player.uniqueId) ?: return ""
            (user.cachedData.metaData.prefix ?: "")
        } catch (_: Throwable) { "" }
    }
}
