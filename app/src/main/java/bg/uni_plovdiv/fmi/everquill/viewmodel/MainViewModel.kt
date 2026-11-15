package bg.uni_plovdiv.fmi.everquill.viewmodel

import androidx.lifecycle.ViewModel
import bg.uni_plovdiv.fmi.everquill.data.entity.Achievement
import bg.uni_plovdiv.fmi.everquill.data.entity.Book
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import bg.uni_plovdiv.fmi.everquill.data.entity.Quest
import bg.uni_plovdiv.fmi.everquill.data.entity.UserProfile
import bg.uni_plovdiv.fmi.everquill.data.repository.ReadingRepository
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val repository = ReadingRepository()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val books: StateFlow<List<Book>> = repository.books
    val quests: StateFlow<List<Quest>> = repository.quests
    val achievements: StateFlow<List<Achievement>> = repository.achievements

    fun addBook(title: String, author: String, totalPages: Int, currentPage: Int, status: BookStatus) {
        repository.addBook(title, author, totalPages, currentPage, status)
    }

    fun updateBookProgress(bookId: String, newCurrentPage: Int) {
        repository.updateBookProgress(bookId, newCurrentPage)
    }

    fun recordReadingSession(bookId: String, pagesRead: Int) {
        repository.recordReadingSession(bookId, pagesRead)
    }

    fun toggleQuest(questId: String) {
        repository.toggleQuest(questId)
    }
}
