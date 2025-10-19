package kr.jjory.jchatproxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.plugin.PluginContainer
import org.slf4j.Logger
import java.nio.file.Path

@Plugin(id = "jchatproxy", name = "JChatProxy", version = "1.0.2", authors = ["jjory"])
class JChatProxy @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    private val container: PluginContainer,
    @DataDirectory private val dataDir: Path
) {
    @Inject lateinit var pluginMessaging: PluginMessaging
    lateinit var config: ProxyConfig
    lateinit var store: StateStore
    lateinit var router: Router
    lateinit var moderation: Moderation
    lateinit var commands: ProxyCommands

    @Subscribe fun onInit(e: ProxyInitializeEvent) {
        this.config = ProxyConfig.load(dataDir, JChatProxy::class.java.classLoader)
        this.store = StateStore(config)
        this.router = Router(server, logger, config)
        this.moderation = Moderation(config, store, logger)
        this.pluginMessaging.init(server, logger, config, store, router, moderation, container)
        this.pluginMessaging.register()
        this.commands = ProxyCommands(server, logger, moderation, router, config, container)
        commands.register()
        logger.info("[JChatProxy] initialized. dataDir=$dataDir channel=${config.channel}")
    }
}
