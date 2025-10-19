package kr.jjory.jchat.service

import kr.jjory.jchat.model.ChatMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ChatModeService(private val data: PlayerDataService, private val net: GlobalMessenger) {
    private val modes = ConcurrentHashMap<UUID, ChatMode>()
    fun get(uuid: UUID): ChatMode = modes[uuid] ?: run { val loaded = data.loadMode(uuid)?.let { ChatMode.valueOf(it) }; if (loaded != null) { modes[uuid] = loaded; loaded } else ChatMode.GLOBAL }
    fun set(uuid: UUID, mode: ChatMode) { modes[uuid] = mode; data.saveMode(uuid, mode.name); net.send(kr.jjory.jchat.common.Payloads.mode(uuid.toString(), mode.name)) }
}
