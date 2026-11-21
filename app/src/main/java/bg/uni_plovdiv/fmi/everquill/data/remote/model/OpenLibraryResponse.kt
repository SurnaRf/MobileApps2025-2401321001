package bg.uni_plovdiv.fmi.everquill.data.remote.model

import com.google.gson.annotations.SerializedName

data class OpenLibrarySearchResponse(
    @SerializedName("docs")
    val docs: List<BookDocDto>?
)

data class BookDocDto(
    @SerializedName("key")
    val key: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("author_name")
    val authorName: List<String>?,
    @SerializedName("first_publish_year")
    val firstPublishYear: Int?,
    @SerializedName("number_of_pages_median")
    val numberOfPagesMedian: Int?,
    @SerializedName("cover_i")
    val coverId: Long?,
    @SerializedName("publisher")
    val publisherList: List<String>?,
    @SerializedName("subject")
    val subjectList: List<String>?
) {
    val authorString: String
        get() = authorName?.joinToString(", ") ?: "Unknown Author"

    val coverUrl: String?
        get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
}
