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
import bg.uni_plovdiv.fmi.everquill.data.repository.MonthlyStat
import bg.uni_plovdiv.fmi.everquill.data.repository.ReadingInsights
import bg.uni_plovdiv.fmi.everquill.data.repository.ReadingRepository
import bg.uni_plovdiv.fmi.everquill.data.repository.WeeklyOverviewData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReadingRepository(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val books: StateFlow<List<Book>> = repository.books
    val quests: StateFlow<List<Quest>> = repository.quests
    val achievements: StateFlow<List<Achievement>> = repository.achievements
    val allSessions: StateFlow<List<ReadingSessionEntity>> = repository.allSessions
    val newlyUnlockedAchievements: SharedFlow<Achievement> = repository.newlyUnlockedAchievements

    fun getSessionsForBook(bookId: String): Flow<List<ReadingSessionEntity>> {
        return repository.getSessionsForBook(bookId)
    }

    fun calculateCurrentStreak(sessions: List<ReadingSessionEntity>): Int {
        return repository.calculateCurrentStreak(sessions)
    }

    fun calculateLongestStreak(sessions: List<ReadingSessionEntity>): Int {
        return repository.calculateLongestStreak(sessions)
    }

    fun calculatePagesReadToday(sessions: List<ReadingSessionEntity>): Int {
        return repository.calculatePagesReadToday(sessions)
    }

    fun calculateWeeklyOverview(sessions: List<ReadingSessionEntity>, dailyGoal: Int): WeeklyOverviewData {
        return repository.calculateWeeklyOverview(sessions, dailyGoal)
    }

    fun calculateMonthlyStats(sessions: List<ReadingSessionEntity>): List<MonthlyStat> {
        return repository.calculateMonthlyStats(sessions)
    }

    fun calculateGenreDistribution(books: List<Book>): Map<String, Int> {
        return repository.calculateGenreDistribution(books)
    }

    fun calculateReadingInsights(sessions: List<ReadingSessionEntity>, books: List<Book>, dailyGoal: Int): ReadingInsights {
        return repository.calculateReadingInsights(sessions, books, dailyGoal)
    }

    fun updateDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(newGoal)
        }
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
