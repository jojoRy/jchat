package kr.jjory.jchat.service

import kr.jjory.jchat.model.ChatMode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatModeService(private val data: PlayerDataService, private val net: GlobalMessenger) {
    private val modes = ConcurrentHashMap<UUID, ChatMode>()
    fun get(uuid: UUID): ChatMode = modes[uuid] ?: run {
        val loaded = data.loadMode(uuid)?.let { runCatching { ChatMode.valueOf(it) }.getOrNull() }
        val resolved = loaded ?: ChatMode.GLOBAL
        modes[uuid] = resolved
        resolved
    }

    fun set(uuid: UUID, mode: ChatMode) {
        update(uuid, mode, propagate = true)
    }

    fun applyRemote(uuid: UUID, mode: ChatMode) {
        update(uuid, mode, propagate = false)
    }

    private fun update(uuid: UUID, mode: ChatMode, propagate: Boolean) {
        val previous = modes.put(uuid, mode)
        data.saveMode(uuid, mode.name)
        if (propagate && previous != mode) {
            net.send(kr.jjory.jchat.common.Payloads.mode(uuid.toString(), mode.name))
        }
    }
}
