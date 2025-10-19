package kr.jjory.jchatproxy

class Moderation(private val cfg: ProxyConfig, private val store: StateStore, private val logger: org.slf4j.Logger) {
    private val regexes = cfg.filterPatterns.mapNotNull { try { Regex(it) } catch (_: Throwable) { null } }.toList()
    fun isMuted(uuidOrName: String): Boolean = cfg.mutedUuids.contains(uuidOrName)
    fun mute(uuid: String) { cfg.mutedUuids.add(uuid); logger.info("[JChatProxy] muted $uuid") }
    fun unmute(uuid: String) { cfg.mutedUuids.remove(uuid); logger.info("[JChatProxy] unmuted $uuid") }
    fun filter(message: String): String? {
        if (!cfg.filterEnabled) return message
        var m = message; for (r in regexes) { if (r.containsMatchIn(m)) { m = m.replace(r, cfg.filterRedact) } }
        return m
    }
}
