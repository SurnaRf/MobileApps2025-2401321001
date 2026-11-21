package bg.uni_plovdiv.fmi.everquill.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bg.uni_plovdiv.fmi.everquill.data.entity.Book
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import bg.uni_plovdiv.fmi.everquill.data.remote.OpenLibraryClient
import bg.uni_plovdiv.fmi.everquill.data.remote.model.BookDocDto
import bg.uni_plovdiv.fmi.everquill.data.repository.ReadingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReadingRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<BookDocDto>>(emptyList())
    val searchResults: StateFlow<List<BookDocDto>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetState() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun searchBooks() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "Please enter a title or author to search."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val response = OpenLibraryClient.apiService.searchBooks(query)
                _searchResults.value = response.docs ?: emptyList()
                if (response.docs.isNullOrEmpty()) {
                    _errorMessage.value = "No books found for \"$query\"."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Network error: ${e.localizedMessage ?: "Please check your connection."}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addBookToLibrary(bookDto: BookDocDto, existingBooks: List<Book>) {
        val title = bookDto.title?.trim() ?: "Untitled"
        val author = bookDto.authorString
        val totalPages = bookDto.numberOfPagesMedian ?: 250
        val coverUrl = bookDto.coverUrl
        val publisher = bookDto.publisherList?.firstOrNull()
        val genre = bookDto.subjectList?.take(3)?.joinToString(", ")
        val publishYear = bookDto.firstPublishYear

        Log.d("BookMetadataDebug", "DTO publisherList: ${bookDto.publisherList}, subjectList: ${bookDto.subjectList}")
        Log.d("BookMetadataDebug", "Extracted publisher: $publisher, genre: $genre, publishYear: $publishYear")

        // Duplicate check based on title and author
        val isDuplicate = existingBooks.any { 
            it.title.equals(title, ignoreCase = true) && it.author.equals(author, ignoreCase = true)
        }

        if (isDuplicate) {
            _errorMessage.value = "\"$title\" is already in your library."
            return
        }

        viewModelScope.launch {
            try {
                repository.addBook(
                    title = title,
                    author = author,
                    totalPages = totalPages,
                    currentPage = 0,
                    status = BookStatus.TO_READ,
                    coverUrl = coverUrl,
                    publisher = publisher,
                    genre = genre,
                    publishYear = publishYear
                )
                _successMessage.value = "\"$title\" added to your library!"
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add book: ${e.localizedMessage}"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
