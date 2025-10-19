package kr.jjory.jchatproxy

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.yaml.snakeyaml.Yaml

class ProxyConfig(
    val channel: String,
    val mirrorGlobal: Boolean,
    val mirrorLocal: Boolean,
    val mirrorAdmin: Boolean,
    val mirrorWhisper: Boolean,
    val mirrorAnnounce: Boolean,
    val mirrorParty: Boolean,
    val mirrorGuild: Boolean,
    val filterEnabled: Boolean,
    val filterPatterns: List<String>,
    val filterRedact: String,
    val routingGlobalAllow: List<String>,
    val routingLocalAllow: List<String>,
    val routingAdminAllow: List<String>,
    val mutedUuids: MutableSet<String>,
    val preferDisplay: Boolean
) {
    companion object {
        fun load(dataDir: Path, cl: ClassLoader): ProxyConfig {
            val cfgPath = dataDir.resolve("config.yml")
            if (!Files.exists(cfgPath)) {
                Files.createDirectories(dataDir)
                cl.getResourceAsStream("config.yml").use { input ->
                    requireNotNull(input) { "config.yml resource not found" }
                    Files.copy(input, cfgPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
            val yaml = Yaml()
            val map = Files.newBufferedReader(cfgPath).use { yaml.load<Map<String, Any>>(it) }

            val channel = (map["plugin-channel"] as? String) ?: "jchat:main"
            val log = map["log"] as? Map<String, Any> ?: emptyMap()
            val mirrorGlobal = log["mirror-global"] as? Boolean ?: true
            val mirrorLocal = log["mirror-local"] as? Boolean ?: true
            val mirrorAdmin = log["mirror-admin"] as? Boolean ?: true
            val mirrorWhisper = log["mirror-whisper"] as? Boolean ?: true
            val mirrorAnnounce = log["mirror-announce"] as? Boolean ?: true
            val mirrorParty = log["mirror-party"] as? Boolean ?: true
            val mirrorGuild = log["mirror-guild"] as? Boolean ?: true

            val filters = map["filters"] as? Map<String, Any> ?: emptyMap()
            val filterEnabled = filters["enabled"] as? Boolean ?: true
            val filterRedact = filters["redact"] as? String ?: "[검열됨]"
            val filterPatterns = (filters["patterns"] as? List<*>)?.map { it.toString() } ?: emptyList()

            val routing = map["routing"] as? Map<String, Any> ?: emptyMap()
            val routingGlobalAllow = (routing["global-allow"] as? List<*>)?.map { it.toString() } ?: emptyList()
            val routingLocalAllow = (routing["local-allow"] as? List<*>)?.map { it.toString() } ?: emptyList()
            val routingAdminAllow = (routing["admin-allow"] as? List<*>)?.map { it.toString() } ?: emptyList()

            val mute = map["mute"] as? Map<String, Any> ?: emptyMap()
            val muted = ((mute["uuids"] as? List<*>)?.map { it.toString() } ?: emptyList()).toMutableSet()

            val prefer = map["prefer-displayname"] as? Boolean ?: true

            return ProxyConfig(channel, mirrorGlobal, mirrorLocal, mirrorAdmin, mirrorWhisper, mirrorAnnounce, mirrorParty, mirrorGuild,
                filterEnabled, filterPatterns, filterRedact, routingGlobalAllow, routingLocalAllow, routingAdminAllow,
                muted, prefer)
        }
    }
}
