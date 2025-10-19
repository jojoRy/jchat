package kr.jjory.jchat.service

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerRegistry {
    private val plain = PlainTextComponentSerializer.plainText()
    private val byKey = ConcurrentHashMap<String, UUID>()
    fun put(p: Player) { byKey[plain.serialize(p.displayName())] = p.uniqueId; byKey[p.name] = p.uniqueId; byKey[p.uniqueId.toString()] = p.uniqueId }
    fun remove(p: Player) { byKey.entries.removeIf { it.value == p.uniqueId } }
    fun findUuid(key: String): UUID? { byKey[key]?.let { return it }; return byKey.entries.firstOrNull { it.key.equals(key, true) }?.value }
}
