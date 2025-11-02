package kr.jjory.jchatproxy

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.RawCommand
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import kr.jjory.jchat.common.AnnounceFormatter
import kr.jjory.jchat.common.ColorCodeFormatter

class ProxyCommands(private val server: ProxyServer, private val logger: org.slf4j.Logger, private val moderation: Moderation, private val router: Router, private val cfg: ProxyConfig, private val container: PluginContainer) {
    fun register() { server.commandManager.register(server.commandManager.metaBuilder("jchatproxy").plugin(container).build(), Root()) }
    inner class Root : RawCommand {
        override fun execute(invocation: RawCommand.Invocation) {
            val src = invocation.source(); val args = invocation.arguments().split(" ").filter { it.isNotBlank() }
            if (args.isEmpty()) { help(src); return }
            when (args[0].lowercase()) {
                "mute" -> { if (args.size < 2) { reply(src, "Usage: /jchatproxy mute <uuid-or-name>"); return }; moderation.mute(args[1]); reply(src, "Muted ${args[1]}") }
                "unmute" -> { if (args.size < 2) { reply(src, "Usage: /jchatproxy unmute <uuid-or-name>"); return }; moderation.unmute(args[1]); reply(src, "Unmuted ${args[1]}") }
                "announce" -> {
                    if (args.size < 2) { reply(src, "Usage: /jchatproxy announce <message>"); return }
                    val msg = args.drop(1).joinToString(" ")
                    val allowColors = when (src) {
                        is Player -> src.hasPermission("jchat.admin")
                        else -> true
                    }
                    val processed = ColorCodeFormatter.apply(msg, allowColors)
                    val padded = AnnounceFormatter.surroundWithBlankLines(processed)
                    router.broadcast(cfg.channel, kr.jjory.jchat.common.Payloads.announce(padded), "GLOBAL")
                    reply(src, "Announced.")
                }else -> help(src)
            }
        }
        private fun help(src: CommandSource) { reply(src, "/jchatproxy mute <uuid-or-name>\n/jchatproxy unmute <uuid-or-name>\n/jchatproxy announce <message>") }
        private fun reply(src: CommandSource, text: String) { src.sendPlainMessage(text) }
    }
}
