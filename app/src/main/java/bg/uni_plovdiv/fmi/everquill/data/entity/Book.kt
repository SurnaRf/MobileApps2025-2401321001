package bg.uni_plovdiv.fmi.everquill.data.entity

enum class BookStatus(val displayName: String) {
    TO_READ("To Read"),
    READING("Reading"),
    COMPLETED("Completed"),
    DNF("DNF")
}

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int,
    val status: BookStatus,
    val dateAdded: Long = System.currentTimeMillis(),
    val coverUrl: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val publishYear: Int? = null
) {
    val progressPercent: Float
        get() = if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f
}
