package kr.jjory.jchat.common

object AnnounceFormatter {
    fun surroundWithBlankLines(message: String): String {
        val core = message.trim('\r', '\n')
        return buildString {
            appendLine()
            append(core)
            appendLine()
            appendLine()
        }
    }
}