package kr.jjory.jchat.service

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method
import java.lang.reflect.Modifier


class PartyGuildService(private val plugin: Plugin) {
    @Volatile private var hook: PartyGuildHook? = null

    init {
        plugin.server.pluginManager.registerEvents(object : Listener {
            @EventHandler fun onPluginEnable(e: PluginEnableEvent) {
                if (e.plugin.name.equals("MMOCore", true)) {
                    hook = null
                    ensureHook()
                }
            }
            @EventHandler fun onPluginDisable(e: PluginDisableEvent) {
                if (e.plugin.name.equals("MMOCore", true)) {
                    hook = null
                }
            }
        }, plugin)
        ensureHook()
    }

    val isHooked: Boolean get() = ensureHook() != null
    val isPartyChatAvailable: Boolean get() = ensureHook()?.supportsParty == true
    val isGuildChatAvailable: Boolean get() = ensureHook()?.supportsGuild == true

    fun partyKey(player: Player): String? = ensureHook()?.partyKey(player)
    fun guildKey(player: Player): String? = ensureHook()?.guildKey(player)

    private fun ensureHook(): PartyGuildHook? {
        hook?.let { return it }
        hook = createHook()
        return hook
    }

    private fun createHook(): PartyGuildHook? {
        val mmocore = plugin.server.pluginManager.getPlugin("MMOCore") ?: return null
        if (!mmocore.isEnabled) return null
        return runCatching { MMOCoreHook(plugin, mmocore) }
            .onSuccess {
                plugin.logger.info(
                    "MMOCore 파티/길드 채팅 연동이 활성화되었습니다. (파티=${if (it.supportsParty) "활성" else "비활성"}, 길드=${if (it.supportsGuild) "활성" else "비활성"})"
                )
            }
            .onFailure { err ->
                plugin.logger.log(java.util.logging.Level.WARNING, "MMOCore API hook failed", err)
            }
            .getOrNull()
    }
}

private interface PartyGuildHook {
    val supportsParty: Boolean
    val supportsGuild: Boolean

    fun partyKey(player: Player): String?
    fun guildKey(player: Player): String?
}

private class MMOCoreHook(private val owner: Plugin, private val mmocore: Plugin) : PartyGuildHook {
    private data class Resolver(val target: Any?, val method: Method) {
        fun invoke(arg: Any?): Any? = runCatching { method.invoke(target, arg) }.getOrNull()
    }

    private val logger = owner.logger

    private val playerDataClass: Class<*>? = listOf(
        "net.Indyuce.mmocore.api.player.PlayerData",
        "net.indyuce.mmocore.api.player.PlayerData",
        "net.Indyuce.mmocore.player.PlayerData",
        "io.lumine.mythic.lib.api.player.PlayerData"
    ).firstNotNullOfOrNull { name ->
        runCatching { Class.forName(name, false, mmocore.javaClass.classLoader) }.getOrNull()
    }

    private val managerCandidates: List<Any> = buildList {
        add(mmocore)
        collectFieldTargets(mmocore)
        collectMethodTargets(mmocore)
    }

    private val resolveByPlayer: Resolver? = findResolver(Player::class.java)
    private val resolveByUuid: Resolver? = findResolver(java.util.UUID::class.java)

    private val playerDataType: Class<*>? = playerDataClass
        ?: resolveByPlayer?.method?.returnType
        ?: resolveByUuid?.method?.returnType

    private val hasParty: Method? = findZeroArgMethod(playerDataType, listOf("hasParty", "inParty", "isInParty", "hasJoinedParty"))
    private val getParty: Method? = findZeroArgMethod(playerDataType, listOf("getParty", "getCurrentParty", "party", "getPartyInstance"))
    private val hasGuild: Method? = findZeroArgMethod(playerDataType, listOf("hasGuild", "inGuild", "isInGuild", "hasJoinedGuild"))
    private val getGuild: Method? = findZeroArgMethod(playerDataType, listOf("getGuild", "getCurrentGuild", "guild", "getGuildInstance"))

    override val supportsParty: Boolean = playerDataType != null && (resolveByPlayer != null || resolveByUuid != null) && hasParty != null && getParty != null
    override val supportsGuild: Boolean = supportsParty && hasGuild != null && getGuild != null

    init {
        if (playerDataType == null) {
            logger.warning("MMOCore 연동 진단: PlayerData 타입을 찾을 수 없습니다.")
        }
        if (resolveByPlayer == null && resolveByUuid == null) {
            logger.warning("MMOCore 연동 진단: 플레이어 데이터를 조회할 수 있는 메서드를 찾지 못했습니다.")
        }
        if (hasParty == null || getParty == null) {
            logger.warning("MMOCore 연동 진단: 파티 정보에 접근할 수 있는 메서드를 찾지 못했습니다.")
        }
        if (hasGuild == null || getGuild == null) {
            logger.warning("MMOCore 연동 진단: 길드 정보에 접근할 수 있는 메서드를 찾지 못했습니다.")
        }
    }


    override fun partyKey(player: Player): String? {
        if (!supportsParty || hasParty == null || getParty == null) return null
        val data = resolve(player) ?: return null
        val hasPartyMethod = hasParty ?: return null
        val joinedValue = runCatching { hasPartyMethod.invoke(data) }.getOrNull()
        val joined = (joinedValue as? Boolean) ?: (joinedValue as? java.lang.Boolean)?.booleanValue() ?: return null
        if (!joined) return null
        val partyMethod = getParty ?: return null
        val party = runCatching { partyMethod.invoke(data) }.getOrNull() ?: return null
        return extractKey(party)
    }

    override fun guildKey(player: Player): String? {
        if (!supportsGuild || hasGuild == null || getGuild == null) return null
        val data = resolve(player) ?: return null
        val hasGuildMethod = hasGuild ?: return null
        val joinedValue = runCatching { hasGuildMethod.invoke(data) }.getOrNull()
        val joined = (joinedValue as? Boolean) ?: (joinedValue as? java.lang.Boolean)?.booleanValue() ?: return null
        if (!joined) return null
        val guildMethod = getGuild ?: return null
        val guild = runCatching { guildMethod.invoke(data) }.getOrNull() ?: return null
        return extractKey(guild)
    }

    private fun resolve(player: Player): Any? {
        resolveByPlayer?.let { resolver ->
            resolver.invoke(player)?.let { return it }
        }
        resolveByUuid?.let { resolver ->
            resolver.invoke(player.uniqueId)?.let { return it }
        }
        logger.fine("MMOCore 연동 진단: ${player.name}의 PlayerData를 찾지 못했습니다.")
        return null
    }

    private fun findResolver(parameter: Class<*>): Resolver? {
        playerDataClass?.methods?.firstOrNull { method ->
            Modifier.isStatic(method.modifiers) && method.parameterCount == 1 && method.parameterTypes[0].isAssignableFrom(parameter)
        }?.let { method ->
            method.isAccessible = true
            return Resolver(null, method)
        }

        managerCandidates.forEach { target ->
            val type = target::class.java
            type.methods.firstOrNull { method ->
                method.parameterCount == 1 && method.parameterTypes[0].isAssignableFrom(parameter) &&
                        (playerDataClass == null || playerDataClass.isAssignableFrom(method.returnType) || method.returnType.name.contains("PlayerData", true))
            }?.let { method ->
                method.isAccessible = true
                return Resolver(target, method)
            }
        }
        return null
    }

    private fun findZeroArgMethod(type: Class<*>?, candidates: List<String>): Method? {
        if (type == null) return null
        return candidates.asSequence()
            .mapNotNull { name ->
                type.methods.firstOrNull { method ->
                    method.parameterCount == 0 && method.name.equals(name, true)
                }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }
            ?: type.methods.firstOrNull { method ->
                method.parameterCount == 0 && candidates.any { keyword ->
                    method.name.contains(
                        keyword.replace(
                            "get",
                            ""
                        ), true
                    )
                }
            }?.apply { isAccessible = true }
    }

    private fun extractKey(target: Any?): String? {
        if (target == null) return null
        if (target is java.util.Optional<*>) {
            return extractKey(target.orElse(null))
        }
        val type = target::class.java
        val id = type.methods.firstOrNull { it.name.equals("getId", true) && it.parameterCount == 0 }
            ?.apply { isAccessible = true }
            ?.let { runCatching { it.invoke(target) }.getOrNull() }
        if (id != null) return id.toString()
        val name = type.methods.firstOrNull { it.name.equals("getName", true) && it.parameterCount == 0 }
            ?.apply { isAccessible = true }
            ?.let { runCatching { it.invoke(target) }.getOrNull() }
        if (name != null) return name.toString()
        val identifier = type.methods.firstOrNull { method ->
            method.parameterCount == 0 && method.returnType == String::class.java && method.name.contains("key", true)
        }?.apply { isAccessible = true }?.let { runCatching { it.invoke(target) }.getOrNull() }
        if (identifier != null) return identifier.toString()
        return target.toString()
    }

    private fun MutableList<Any>.collectFieldTargets(instance: Plugin) {
        instance.javaClass.declaredFields.forEach { field ->
            if (!field.type.name.contains("Player", true) &&
                (field.name.contains("playerData", true) || field.name.contains("dataManager", true) || field.type.name.contains("PlayerData", true) || field.type.name.contains("PlayerManager", true))
            ) {
                field.isAccessible = true
                val targetInstance = if (Modifier.isStatic(field.modifiers)) null else instance
                val value = runCatching { field.get(targetInstance) }.getOrNull()
                when (value) {
                    is java.util.Optional<*> -> value.orElse(null)?.let { add(it) }
                    null -> {}
                    else -> add(value)
                }
            }
        }
    }

    private fun MutableList<Any>.collectMethodTargets(instance: Plugin) {
        instance.javaClass.methods.filter { method ->
            method.parameterCount == 0 && (method.name.contains("playerData", true) || method.name.contains("dataManager", true) || method.returnType.name.contains("PlayerData", true) || method.returnType.name.contains("PlayerManager", true))
        }.forEach { method ->
            method.isAccessible = true
            val targetInstance = if (Modifier.isStatic(method.modifiers)) null else instance
            val value = runCatching { method.invoke(targetInstance) }.getOrNull()
            when (value) {
                is java.util.Optional<*> -> value.orElse(null)?.let { add(it) }
                null -> {}
                else -> add(value)
            }
        }
    }
}
