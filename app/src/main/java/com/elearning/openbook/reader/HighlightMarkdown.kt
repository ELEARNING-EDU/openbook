package com.elearning.openbook.reader

/**
 * One highlight flattened for the Markdown export: its chapter title is already resolved
 * by the caller, so this file has no Android types and its exact bytes can be tested.
 */
data class MarkdownHighlight(
    val chapterIndex: Int,
    val chapterTitle: String,
    val startOffset: Int,
    val text: String,
    val note: String = "",
)

/**
 * The book's highlights as Markdown: a title line, the author when known, then one
 * section per chapter in reading order, each highlight quoted and its note below it.
 *
 * The output ends with exactly one newline, and a note's paragraph is omitted when the
 * note is blank. Same bytes on Android and iOS, so it is built here only.
 */
fun highlightsMarkdown(
    title: String,
    author: String,
    highlights: List<MarkdownHighlight>,
): String {
    val out = StringBuilder()
    out.append("# ").append(title).append('\n')
    if (author.isNotBlank()) out.append(author).append('\n')
    out.append('\n')
    val sorted = highlights.sortedWith(compareBy({ it.chapterIndex }, { it.startOffset }))
    for ((_, group) in sorted.groupBy { it.chapterIndex }) {
        out.append("## ").append(group.first().chapterTitle).append("\n\n")
        for (highlight in group) {
            for (line in highlight.text.lines()) out.append("> ").append(line).append('\n')
            out.append('\n')
            if (highlight.note.isNotBlank()) out.append(highlight.note).append('\n').append('\n')
        }
    }
    return out.toString().trimEnd('\n') + "\n"
}
