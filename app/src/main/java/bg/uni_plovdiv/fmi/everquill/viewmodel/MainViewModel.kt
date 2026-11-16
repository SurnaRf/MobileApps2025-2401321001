package bg.uni_plovdiv.fmi.everquill.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bg.uni_plovdiv.fmi.everquill.data.database.entity.ReadingSessionEntity
import bg.uni_plovdiv.fmi.everquill.data.entity.Achievement
import bg.uni_plovdiv.fmi.everquill.data.entity.Book
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import bg.uni_plovdiv.fmi.everquill.data.entity.Quest
import bg.uni_plovdiv.fmi.everquill.data.entity.UserProfile
import bg.uni_plovdiv.fmi.everquill.data.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReadingRepository(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val books: StateFlow<List<Book>> = repository.books
    val quests: StateFlow<List<Quest>> = repository.quests
    val achievements: StateFlow<List<Achievement>> = repository.achievements

    fun getSessionsForBook(bookId: String): Flow<List<ReadingSessionEntity>> {
        return repository.getSessionsForBook(bookId)
    }

    fun addBook(title: String, author: String, totalPages: Int, currentPage: Int, status: BookStatus) {
        viewModelScope.launch {
            repository.addBook(title, author, totalPages, currentPage, status)
        }
    }

    fun updateBookProgress(bookId: String, newCurrentPage: Int) {
        viewModelScope.launch {
            repository.updateBookProgress(bookId, newCurrentPage)
        }
    }

    fun updateBookDetails(bookId: String, title: String, author: String, totalPages: Int, status: BookStatus) {
        viewModelScope.launch {
            repository.updateBookDetails(bookId, title, author, totalPages, status)
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
        }
    }

    fun recordReadingSession(bookId: String, pagesRead: Int) {
        viewModelScope.launch {
            repository.recordReadingSession(bookId, pagesRead)
        }
    }

    fun toggleQuest(questId: String) {
        viewModelScope.launch {
            repository.toggleQuest(questId)
        }
    }
}
