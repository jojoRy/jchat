package kr.jjory.jchatproxy

import java.util.*
import java.util.concurrent.ConcurrentHashMap

enum class ChatMode { GLOBAL, LOCAL, ADMIN }

class StateStore(private val cfg: ProxyConfig) {
    private val modes = ConcurrentHashMap<UUID, ChatMode>()
    private val nameIndex = ConcurrentHashMap<String, UUID>()
    private val lastReply = ConcurrentHashMap<UUID, UUID>()
    fun setMode(uuid: UUID, mode: ChatMode) { modes[uuid] = mode }
    fun getMode(uuid: UUID): ChatMode = modes[uuid] ?: ChatMode.GLOBAL
    fun index(uuid: UUID, name: String, display: String) { nameIndex[name.lowercase()] = uuid; nameIndex[display.lowercase()] = uuid; nameIndex[uuid.toString()] = uuid }
    fun unindex(uuid: UUID) { nameIndex.entries.removeIf { it.value == uuid }; modes.remove(uuid); lastReply.remove(uuid) }
    fun find(key: String): UUID? = nameIndex[key.lowercase()]
    fun setReply(a: UUID, b: UUID) { lastReply[a] = b }
    fun getReply(a: UUID): UUID? = lastReply[a]
}
