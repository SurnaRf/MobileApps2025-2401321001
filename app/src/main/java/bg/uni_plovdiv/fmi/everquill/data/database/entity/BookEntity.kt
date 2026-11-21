package bg.uni_plovdiv.fmi.everquill.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import bg.uni_plovdiv.fmi.everquill.data.entity.Book
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
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
    fun toDomain() = Book(
        id = id,
        title = title,
        author = author,
        totalPages = totalPages,
        currentPage = currentPage,
        status = status,
        dateAdded = dateAdded,
        coverUrl = coverUrl,
        publisher = publisher,
        genre = genre,
        publishYear = publishYear
    )

    companion object {
        fun fromDomain(book: Book) = BookEntity(
            id = book.id,
            title = book.title,
            author = book.author,
            totalPages = book.totalPages,
            currentPage = book.currentPage,
            status = book.status,
            dateAdded = book.dateAdded,
            coverUrl = book.coverUrl,
            publisher = book.publisher,
            genre = book.genre,
            publishYear = book.publishYear
        )
    }
}
