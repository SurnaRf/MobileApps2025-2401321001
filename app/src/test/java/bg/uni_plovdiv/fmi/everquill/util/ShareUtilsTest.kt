package bg.uni_plovdiv.fmi.everquill.util

import bg.uni_plovdiv.fmi.everquill.data.entity.Book
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareUtilsTest {

    @Test
    fun testGenerateBookShareText() {
        val book = Book(
            id = "1",
            title = "The Hobbit",
            author = "J.R.R. Tolkien",
            totalPages = 310,
            currentPage = 310,
            status = BookStatus.COMPLETED
        )

        val expected = buildString {
            appendLine("My book recommendation 📚")
            appendLine()
            appendLine("The Hobbit")
            appendLine("by J.R.R. Tolkien")
            appendLine()
            appendLine("Status: Completed")
            appendLine("Progress: 310 / 310 pages")
            appendLine()
            append("Shared from Everquill 🌿")
        }

        val actual = generateBookShareText(book)
        assertEquals(expected, actual)
    }
}
