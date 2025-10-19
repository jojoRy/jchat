package kr.jjory.jchat.common

object Payloads {
    fun global(serverId: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.GLOBAL}|$serverId|$fromName|$fromDisplay|$message"
    fun local(serverId: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.LOCAL}|$serverId|$fromName|$fromDisplay|$message"
    fun admin(serverId: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.ADMIN}|$serverId|$fromName|$fromDisplay|$message"
    fun whisper(serverId: String, fromName: String, targetKey: String, message: String): String =
        "${PacketType.WHISPER}|$serverId|$fromName|$targetKey|$message"
    fun whisperRemote(serverId: String, fromUuid: String, fromName: String, targetKey: String, message: String): String =
        "${PacketType.WHISPER_REMOTE}|$serverId|$fromUuid|$fromName|$targetKey|$message"
    fun mode(uuid: String, mode: String): String = "${PacketType.MODE}|*|$uuid|$mode"
    fun index(serverId: String, uuid: String, name: String, display: String): String =
        "${PacketType.INDEX}|$serverId|$uuid|$name|$display"
    fun announce(message: String): String = "${PacketType.ANNOUNCE}|$message"
    fun party(serverId: String, partyKey: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.PARTY}|$serverId|$partyKey|$fromName|$fromDisplay|$message"
    fun guild(serverId: String, guildKey: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.GUILD}|$serverId|$guildKey|$fromName|$fromDisplay|$message"
}
