package com.stc.terminowo.presentation.components

import com.stc.terminowo.domain.model.Document

/** Excerpt of a document's OCR text around a search hit; [highlights] index into [text]. */
data class SearchSnippet(
    val text: String,
    val highlights: List<IntRange>
)

/**
 * A document matches when every word of the query appears somewhere in its name, comments
 * or OCR text, in any order. OCR often reads form fields out of order ("Jan PESEL: Kowalski"),
 * so matching the query as one phrase would miss e.g. a first + last name.
 */
internal fun Document.matchesSearch(query: String): Boolean {
    val words = queryWords(query)
    if (words.isEmpty()) return false
    val haystack = "$name $myComments $ocrText".foldForSearch()
    return words.all { haystack.contains(it) }
}

/**
 * Returns an excerpt of the OCR text around the first hit, or null when the name and comments
 * alone already contain every query word (the result row explains itself) or nothing matches.
 */
internal fun Document.ocrSnippet(query: String, contextChars: Int = 30): SearchSnippet? {
    val words = queryWords(query)
    if (words.isEmpty()) return null
    val ownText = "$name $myComments".foldForSearch()
    val ocrWords = words.filterNot { ownText.contains(it) }
    if (ocrWords.isEmpty()) return null

    // Folding is char-for-char, so indices in the folded text are valid in the original.
    val foldedOcr = ocrText.foldForSearch()
    val (index, word) = ocrWords
        .mapNotNull { w -> foldedOcr.indexOf(w).takeIf { it >= 0 }?.let { it to w } }
        .minByOrNull { it.first }
        ?: return null
    val matchEnd = index + word.length

    var start = (index - contextChars).coerceAtLeast(0)
    var end = (matchEnd + contextChars).coerceAtMost(ocrText.length)
    // Don't cut words in half at the excerpt edges.
    if (start > 0) {
        val space = ocrText.indexOf(' ', start)
        if (space in start until index) start = space + 1
    }
    if (end < ocrText.length) {
        val space = ocrText.lastIndexOf(' ', end)
        if (space >= matchEnd) end = space
    }

    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < ocrText.length) "…" else ""
    val window = foldedOcr.substring(start, end)
    val highlights = words
        .flatMap { w -> occurrences(window, w).map { it..<(it + w.length) } }
        .sortedBy { it.first }
        .map { (it.first + prefix.length)..(it.last + prefix.length) }
    return SearchSnippet(
        text = prefix + ocrText.substring(start, end) + suffix,
        highlights = highlights
    )
}

private fun occurrences(text: String, word: String): List<Int> = buildList {
    var from = text.indexOf(word)
    while (from >= 0) {
        add(from)
        from = text.indexOf(word, from + word.length)
    }
}

private fun queryWords(query: String): List<String> =
    query.foldForSearch().split(WHITESPACE).filter { it.isNotEmpty() }.distinct()

/** Lowercases and strips Polish diacritics one char at a time, so string length is preserved. */
internal fun String.foldForSearch(): String = buildString(length) {
    for (c in this@foldForSearch) {
        append(
            when (val lower = c.lowercaseChar()) {
                'ą' -> 'a'
                'ć' -> 'c'
                'ę' -> 'e'
                'ł' -> 'l'
                'ń' -> 'n'
                'ó' -> 'o'
                'ś' -> 's'
                'ź', 'ż' -> 'z'
                else -> lower
            }
        )
    }
}

private val WHITESPACE = Regex("\\s+")
