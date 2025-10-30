package kr.jjory.jchat.common

object ColorCodeFormatter {
    private val legacyMap = mapOf(
        '0' to "<black>",
        '1' to "<dark_blue>",
        '2' to "<dark_green>",
        '3' to "<dark_aqua>",
        '4' to "<dark_red>",
        '5' to "<dark_purple>",
        '6' to "<gold>",
        '7' to "<gray>",
        '8' to "<dark_gray>",
        '9' to "<blue>",
        'a' to "<green>",
        'b' to "<aqua>",
        'c' to "<red>",
        'd' to "<light_purple>",
        'e' to "<yellow>",
        'f' to "<white>",
        'k' to "<obfuscated>",
        'l' to "<bold>",
        'm' to "<strikethrough>",
        'n' to "<underlined>",
        'o' to "<italic>",
        'r' to "<reset>"
    )
    private val namedColorPattern = Regex("(?i)(?<!\\\\)<(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|reset)>")
    private val hexColorPattern = Regex("(?i)(?<!\\\\)<#([0-9a-f]{6})>")
    private val colorDirectivePattern = Regex("(?i)(?<!\\\\)<color:(#[0-9a-f]{6}|black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>")

    fun apply(input: String, allowColors: Boolean): String {
        return if (allowColors) translateLegacyCodes(input) else escapeColorTags(input)
    }

    private fun translateLegacyCodes(input: String): String {
        val builder = StringBuilder(input.length)
        var index = 0
        while (index < input.length) {
            val current = input[index]
            if ((current == '&' || current == '\u00A7') && index + 1 < input.length) {
                val code = input[index + 1].lowercaseChar()
                val replacement = legacyMap[code]
                if (replacement != null) {
                    builder.append(replacement)
                    index += 2
                    continue
                }
            }
            builder.append(current)
            index++
        }
        return builder.toString()
    }

    private fun escapeColorTags(input: String): String {
        var result = input
        result = hexColorPattern.replace(result) { "\\\\${it.value}" }
        result = colorDirectivePattern.replace(result) { "\\\\${it.value}" }
        result = namedColorPattern.replace(result) { "\\\\${it.value}" }
        return result
    }
}