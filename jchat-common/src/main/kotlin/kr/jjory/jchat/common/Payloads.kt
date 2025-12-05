package kr.jjory.jchat.common

object Payloads {
    fun global(serverId: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.GLOBAL}|$serverId|$fromName|$fromDisplay|$message"
    fun local(serverId: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.LOCAL}|$serverId|$fromName|$fromDisplay|$message"
    fun admin(serverId: String, fromName: String, fromDisplay: String, message: String): String =
        "${PacketType.ADMIN}|$serverId|$fromName|$fromDisplay|$message"
    fun whisper(serverId: String, fromName: String, fromDisplay: String, targetKey: String, message: String): String =
        "${PacketType.WHISPER}|$serverId|$fromName|$fromDisplay|$targetKey|$message"
    fun whisperRemote(serverId: String, fromUuid: String, fromName: String, fromDisplay: String, targetKey: String, message: String): String =
        "${PacketType.WHISPER_REMOTE}|$serverId|$fromUuid|$fromName|$fromDisplay|$targetKey|$message"
    fun mode(uuid: String, mode: String): String = "${PacketType.MODE}|*|$uuid|$mode"
    fun index(serverId: String, uuid: String, name: String, display: String): String =
        "${PacketType.INDEX}|$serverId|$uuid|$name|$display"
    fun announce(message: String): String = "${PacketType.ANNOUNCE}|$message"
}
