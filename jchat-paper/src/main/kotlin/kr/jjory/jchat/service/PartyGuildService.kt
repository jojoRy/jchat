package kr.jjory.jchat.service

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class PartyGuildService(plugin: Plugin) {
    private val hook: PartyGuildHook? = createHook(plugin)

    val isHooked: Boolean get() = hook != null
    val isPartyChatAvailable: Boolean get() = hook?.supportsParty == true
    val isGuildChatAvailable: Boolean get() = hook?.supportsGuild == true

    fun partyKey(player: Player): String? = hook?.partyKey(player)
    fun guildKey(player: Player): String? = hook?.guildKey(player)

    private fun createHook(plugin: Plugin): PartyGuildHook? {
        val mmocore = plugin.server.pluginManager.getPlugin("MMOCore") ?: return null
        if (!mmocore.isEnabled) return null
        return runCatching { MMOCoreHook() }
            .onFailure { plugin.logger.warning("MMOCore API hook failed: ${it.message}") }
            .getOrNull()
    }
}

private interface PartyGuildHook {
    val supportsParty: Boolean
    val supportsGuild: Boolean

    fun partyKey(player: Player): String?
    fun guildKey(player: Player): String?
}

private class MMOCoreHook : PartyGuildHook {
    private val playerDataClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData")
    private val resolveByPlayer: Method? = playerDataClass.methods.firstOrNull {
        it.name == "get" && it.parameterCount == 1 && Modifier.isStatic(it.modifiers) &&
                Player::class.java.isAssignableFrom(it.parameterTypes[0])
    }?.apply { isAccessible = true }
    private val resolveByUuid: Method? = playerDataClass.methods.firstOrNull {
        it.name == "get" && it.parameterCount == 1 && Modifier.isStatic(it.modifiers) &&
                java.util.UUID::class.java.isAssignableFrom(it.parameterTypes[0])
    }?.apply { isAccessible = true }
    private val hasParty: Method? = playerDataClass.methods.firstOrNull { it.name == "hasParty" && it.parameterCount == 0 }?.apply { isAccessible = true }
    private val getParty: Method? = playerDataClass.methods.firstOrNull { it.name == "getParty" && it.parameterCount == 0 }?.apply { isAccessible = true }
    private val hasGuild: Method? = playerDataClass.methods.firstOrNull { it.name == "hasGuild" && it.parameterCount == 0 }?.apply { isAccessible = true }
    private val getGuild: Method? = playerDataClass.methods.firstOrNull { it.name == "getGuild" && it.parameterCount == 0 }?.apply { isAccessible = true }

    override val supportsParty: Boolean = (resolveByPlayer != null || resolveByUuid != null) && hasParty != null && getParty != null
    override val supportsGuild: Boolean = supportsParty && hasGuild != null && getGuild != null

    override fun partyKey(player: Player): String? {
        if (!supportsParty || hasParty == null || getParty == null) return null
        val data = resolve(player) ?: return null
        val joined = runCatching { hasParty?.invoke(data) as? Boolean }.getOrNull() ?: return null
        if (!joined) return null
        val party = runCatching { getParty?.invoke(data) }.getOrNull() ?: return null
        return extractKey(party)
    }

    override fun guildKey(player: Player): String? {
        if (!supportsGuild || hasGuild == null || getGuild == null) return null
        val data = resolve(player) ?: return null
        val joined = runCatching { hasGuild?.invoke(data) as? Boolean }.getOrNull() ?: return null
        if (!joined) return null
        val guild = runCatching { getGuild?.invoke(data) }.getOrNull() ?: return null
        return extractKey(guild)
    }

    private fun resolve(player: Player): Any? {
        resolveByPlayer?.let { return runCatching { it.invoke(null, player) }.getOrNull() }
        resolveByUuid?.let { return runCatching { it.invoke(null, player.uniqueId) }.getOrNull() }
        return null
    }

    private fun extractKey(target: Any?): String? {
        if (target == null) return null
        val type = target::class.java
        val id = type.methods.firstOrNull { it.name.equals("getId", true) && it.parameterCount == 0 }
            ?.apply { isAccessible = true }
            ?.let { runCatching { it.invoke(target) }.getOrNull() }
        if (id != null) return id.toString()
        val name = type.methods.firstOrNull { it.name.equals("getName", true) && it.parameterCount == 0 }
            ?.apply { isAccessible = true }
            ?.let { runCatching { it.invoke(target) }.getOrNull() }
        if (name != null) return name.toString()
        return target.toString()
    }
}
