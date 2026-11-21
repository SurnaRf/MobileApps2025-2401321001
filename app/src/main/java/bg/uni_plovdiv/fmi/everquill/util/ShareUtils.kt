package bg.uni_plovdiv.fmi.everquill.util

import bg.uni_plovdiv.fmi.everquill.data.entity.Book

fun generateBookShareText(book: Book): String {
    val progressText = if (book.totalPages > 0) {
        "Progress: ${book.currentPage} / ${book.totalPages} pages"
    } else {
        "Progress: ${book.currentPage} pages"
    }

    return buildString {
        appendLine("My book recommendation 📚")
        appendLine()
        appendLine(book.title)
        appendLine("by ${book.author}")
        appendLine()
        appendLine("Status: ${book.status.displayName}")
        appendLine(progressText)
        appendLine()
        append("Shared from Everquill 🌿")
    }
}
