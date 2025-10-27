package kr.jjory.jchat.listener

import kr.jjory.jchat.model.ChatMode
import kr.jjory.jchat.service.*
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerLifecycleListener(private val registry: PlayerRegistry, private val modes: ChatModeService, private val data: PlayerDataService, private val config: ConfigService, private val channel: PluginChannelMessenger) : Listener {
    private val plain = PlainTextComponentSerializer.plainText()
    @EventHandler fun onJoin(e: PlayerJoinEvent) {
        val p = e.player; registry.put(p)
        modes.get(p.uniqueId)
        val display = plain.serialize(p.displayName())
        channel.send(kr.jjory.jchat.common.Payloads.index(config.serverId, p.uniqueId.toString(), p.name, display))
    }
    @EventHandler fun onQuit(e: PlayerQuitEvent) { registry.remove(e.player) }
}
