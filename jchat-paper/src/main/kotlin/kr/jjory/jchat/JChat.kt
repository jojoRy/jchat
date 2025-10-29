package kr.jjory.jchat

import org.bukkit.plugin.java.JavaPlugin
import kr.jjory.jchat.listener.ChatListener
import kr.jjory.jchat.listener.PlayerLifecycleListener
import kr.jjory.jchat.service.*
import kr.jjory.jchat.command.*

class JChat : JavaPlugin() {
    lateinit var configService: ConfigService
    lateinit var dataService: PlayerDataService
    lateinit var registry: PlayerRegistry
    lateinit var chatModeService: ChatModeService
    lateinit var whisperService: WhisperService
    lateinit var prefixResolver: PrefixResolver
    lateinit var globalMessenger: GlobalMessenger
    lateinit var messageLogManager: MessageLogManager
    lateinit var channelMessenger: PluginChannelMessenger

    override fun onEnable() {
        saveDefaultConfig()
        configService = ConfigService(this).also { it.ensureServerId() }
        dataService = PlayerDataService(this)
        registry = PlayerRegistry()
        messageLogManager = MessageLogManager(this)
        globalMessenger = GlobalMessenger(configService, messageLogManager)
        chatModeService = ChatModeService(dataService, globalMessenger)
        prefixResolver = PrefixResolver()
        whisperService = WhisperService(configService, globalMessenger, messageLogManager, prefixResolver, registry)
        channelMessenger = PluginChannelMessenger(this, configService, messageLogManager)

        channelMessenger.registerIncoming { payload ->
            globalMessenger.simulateReceive(payload)
        }
        GlobalMessenger.channelSender = { payload -> channelMessenger.send(payload) }

        server.pluginManager.registerEvents(ChatListener(this, configService, chatModeService, globalMessenger, messageLogManager, prefixResolver), this)
        server.pluginManager.registerEvents(PlayerLifecycleListener(registry, chatModeService, dataService, configService, channelMessenger), this)

        getCommand("전체")?.setExecutor(GlobalCommand(chatModeService))
        getCommand("지역")?.setExecutor(LocalCommand(chatModeService))
        getCommand("관리자")?.setExecutor(AdminCommand(chatModeService))
        getCommand("공지")?.setExecutor(AnnounceCommand(configService, globalMessenger))
        getCommand("귓")?.setExecutor(WhisperCommand(whisperService))
        getCommand("답장")?.setExecutor(ReplyCommand(whisperService))
        getCommand("채팅리로드")?.setExecutor(ReloadCommand(this, configService))
    }
}
