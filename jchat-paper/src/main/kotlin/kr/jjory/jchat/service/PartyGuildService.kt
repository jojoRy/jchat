package kr.jjory.jchat.service

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player

/**
 * MMOCore가 설치되어 있지 않거나 API 버전이 달라도 동작하도록
 * PAPI 기반의 키(파티/길드 식별자)를 추출한다.
 * 사용되는 플레이스홀더 후보(비어 있거나 "none"/"null"이면 무시):
 *  - %mmocore_party_id%, %mmocore_party_name%
 *  - %mmocore_guild_id%, %mmocore_guild_name%
 */
class PartyGuildService {
    private fun parse(p: Player, ph: String): String? = try {
        val v = PlaceholderAPI.setPlaceholders(p, ph).trim()
        if (v.isBlank() || v.equals("none", true) || v.equals("null", true)) null else v
    } catch (_: Throwable) { null }

    fun partyKey(p: Player): String? =
        parse(p, "%mmocore_party_id%") ?: parse(p, "%mmocore_party_name%")

    fun guildKey(p: Player): String? =
        parse(p, "%mmocore_guild_id%") ?: parse(p, "%mmocore_guild_name%")
}
