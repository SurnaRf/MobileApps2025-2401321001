package bg.uni_plovdiv.fmi.everquill.data.remote

import bg.uni_plovdiv.fmi.everquill.data.remote.model.OpenLibrarySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApiService {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fields") fields: String = "title,author_name,first_publish_year,number_of_pages_median,cover_i,publisher,subject,key"
    ): OpenLibrarySearchResponse
}
