package com.rifttally.core.ui

/**
 * The initial shown in a fallback avatar: the first letter or digit of [legendName], or of [fallbackName]
 * when there's no Legend. Uppercased; handles characters outside the BMP. Empty if neither has one.
 */
internal fun avatarInitial(legendName: String?, fallbackName: String): String =
    firstLetterOrDigit(legendName.orEmpty()) ?: firstLetterOrDigit(fallbackName) ?: ""

private fun firstLetterOrDigit(text: String): String? {
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        if (Character.isLetterOrDigit(codePoint)) {
            return String(Character.toChars(codePoint)).uppercase()
        }
        index += Character.charCount(codePoint)
    }
    return null
}
