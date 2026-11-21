package bg.uni_plovdiv.fmi.everquill.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenLibraryClient {
    private const val BASE_URL = "https://openlibrary.org/"

    val apiService: OpenLibraryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenLibraryApiService::class.java)
    }
}
