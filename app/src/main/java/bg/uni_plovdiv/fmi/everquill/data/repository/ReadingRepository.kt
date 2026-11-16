package bg.uni_plovdiv.fmi.everquill.data.repository

import android.content.Context
import android.util.Log
import bg.uni_plovdiv.fmi.everquill.data.database.AppDatabase
import bg.uni_plovdiv.fmi.everquill.data.database.entity.BookEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.ReadingSessionEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.UserProfileEntity
import bg.uni_plovdiv.fmi.everquill.data.entity.Achievement
import bg.uni_plovdiv.fmi.everquill.data.entity.Book
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import bg.uni_plovdiv.fmi.everquill.data.entity.Quest
import bg.uni_plovdiv.fmi.everquill.data.entity.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.UUID

class ReadingRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val bookDao = database.bookDao()
    private val readingSessionDao = database.readingSessionDao()
    private val questDao = database.questDao()
    private val achievementDao = database.achievementDao()
    private val userProfileDao = database.userProfileDao()

    private val externalScope = CoroutineScope(Dispatchers.IO)

    val books: StateFlow<List<Book>> = bookDao.getAllBooks()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = userProfileDao.getUserProfile()
        .map { entity -> entity?.toDomain() ?: UserProfile() }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val quests: StateFlow<List<Quest>> = questDao.getDailyQuests()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<Achievement>> = achievementDao.getAllAchievements()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSessionsForBook(bookId: String): Flow<List<ReadingSessionEntity>> {
        return readingSessionDao.getSessionsForBook(bookId)
    }

    suspend fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        currentPage: Int,
        status: BookStatus,
        coverUrl: String? = null,
        publisher: String? = null,
        genre: String? = null,
        publishYear: Int? = null
    ) {
        withContext(Dispatchers.IO) {
            Log.d("BookMetadataDebug", "Repo addBook received publisher: $publisher, genre: $genre, publishYear: $publishYear")
            val clampedPage = currentPage.coerceIn(0, totalPages)
            val finalStatus = if (clampedPage >= totalPages && totalPages > 0) BookStatus.COMPLETED else status
            val newBook = BookEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                author = author.trim(),
                totalPages = totalPages,
                currentPage = clampedPage,
                status = finalStatus,
                coverUrl = coverUrl,
                publisher = publisher,
                genre = genre,
                publishYear = publishYear
            )
            Log.d("BookMetadataDebug", "Saved BookEntity publisher: ${newBook.publisher}, genre: ${newBook.genre}, publishYear: ${newBook.publishYear}")
            bookDao.insertBook(newBook)
            updateAchievementsSync()
        }
    }

    suspend fun updateBookProgress(bookId: String, newCurrentPage: Int) {
        withContext(Dispatchers.IO) {
            val book = bookDao.getBookById(bookId) ?: return@withContext
            val clampedPage = newCurrentPage.coerceIn(0, book.totalPages)
            val newStatus = if (clampedPage >= book.totalPages && book.totalPages > 0) BookStatus.COMPLETED else book.status
            val updated = book.copy(currentPage = clampedPage, status = newStatus)
            bookDao.updateBook(updated)
            updateAchievementsSync()
        }
    }

    suspend fun updateBookDetails(bookId: String, title: String, author: String, totalPages: Int, status: BookStatus) {
        withContext(Dispatchers.IO) {
            val book = bookDao.getBookById(bookId) ?: return@withContext
            val clampedPage = book.currentPage.coerceAtMost(totalPages)
            val updated = book.copy(
                title = title.trim(),
                author = author.trim(),
                totalPages = totalPages,
                currentPage = clampedPage,
                status = status
            )
            bookDao.updateBook(updated)
            updateAchievementsSync()
        }
    }

    suspend fun deleteBook(bookId: String) {
        withContext(Dispatchers.IO) {
            val book = bookDao.getBookById(bookId) ?: return@withContext
            bookDao.deleteBook(book)
            updateAchievementsSync()
        }
    }

    suspend fun recordReadingSession(bookId: String, pagesRead: Int) {
        if (pagesRead <= 0) return
        withContext(Dispatchers.IO) {
            val book = bookDao.getBookById(bookId) ?: return@withContext
            val startPage = book.currentPage
            val endPage = (startPage + pagesRead).coerceAtMost(book.totalPages)
            val actualPagesRead = endPage - startPage
            if (actualPagesRead <= 0) return@withContext

            val newStatus = if (endPage >= book.totalPages && book.totalPages > 0) BookStatus.COMPLETED else book.status
            val updatedBook = book.copy(currentPage = endPage, status = newStatus)
            bookDao.updateBook(updatedBook)

            val xpEarned = actualPagesRead * 10
            readingSessionDao.insertSession(
                ReadingSessionEntity(
                    bookId = bookId,
                    pagesRead = actualPagesRead,
                    startPage = startPage,
                    endPage = endPage,
                    xpEarned = xpEarned
                )
            )

            var profile = userProfileDao.getUserProfileSync() ?: UserProfileEntity(
                id = 1, name = "Reader", level = 1, currentXp = 0, maxXpForNextLevel = 100, pagesReadToday = 0, streakDays = 1, lastReadingDate = System.currentTimeMillis()
            )

            var newXp = profile.currentXp + xpEarned
            var newLevel = profile.level
            var maxLevelXp = profile.maxXpForNextLevel

            while (newXp >= maxLevelXp) {
                newXp -= maxLevelXp
                newLevel += 1
                maxLevelXp += 100
            }

            val newPagesToday = profile.pagesReadToday + actualPagesRead
            profile = profile.copy(
                level = newLevel,
                currentXp = newXp,
                maxXpForNextLevel = maxLevelXp,
                pagesReadToday = newPagesToday
            )
            userProfileDao.updateUserProfile(profile)

            // Update quest q1 ("Read 20 pages today")
            val quest1 = questDao.getQuestById("q1")
            if (quest1 != null && !quest1.isCompleted) {
                val updatedProgress = (quest1.currentProgress + actualPagesRead).coerceAtMost(quest1.targetPages)
                val isCompleted = updatedProgress >= quest1.targetPages
                questDao.insertOrUpdateQuest(
                    quest1.copy(currentProgress = updatedProgress, isCompleted = isCompleted)
                )
                if (isCompleted) {
                    var xp = profile.currentXp + quest1.xpReward
                    var lvl = profile.level
                    var maxXp = profile.maxXpForNextLevel
                    if (xp >= maxXp) {
                        xp -= maxXp
                        lvl += 1
                        maxXp += 100
                    }
                    userProfileDao.updateUserProfile(
                        profile.copy(level = lvl, currentXp = xp, maxXpForNextLevel = maxXp)
                    )
                }
            }

            updateAchievementsSync()
        }
    }

    suspend fun toggleQuest(questId: String) {
        withContext(Dispatchers.IO) {
            val quest = questDao.getQuestById(questId) ?: return@withContext
            val newState = !quest.isCompleted
            val updatedQuest = quest.copy(isCompleted = newState, currentProgress = if (newState) quest.targetPages else quest.currentProgress)
            questDao.insertOrUpdateQuest(updatedQuest)

            if (newState) {
                val profile = userProfileDao.getUserProfileSync() ?: return@withContext
                var xp = profile.currentXp + quest.xpReward
                var lvl = profile.level
                var maxXp = profile.maxXpForNextLevel
                while (xp >= maxXp) {
                    xp -= maxXp
                    lvl += 1
                    maxXp += 100
                }
                userProfileDao.updateUserProfile(
                    profile.copy(level = lvl, currentXp = xp, maxXpForNextLevel = maxXp)
                )
            }
        }
    }

    private suspend fun updateAchievementsSync() {
        val allBooks = bookDao.getAllBooksSync()
        val totalPagesRead = allBooks.sumOf { it.currentPage }
        val completedBooks = allBooks.count { it.status == BookStatus.COMPLETED }

        val achievements = achievementDao.getAllAchievementsSync()
        for (ach in achievements) {
            when (ach.id) {
                "a2" -> { // Bookworm (100 pages)
                    val unlocked = totalPagesRead >= 100
                    achievementDao.updateAchievement(
                        ach.copy(currentProgress = totalPagesRead, isUnlocked = unlocked)
                    )
                }
                "a3" -> { // Getting Started (1 book completed)
                    val unlocked = completedBooks >= 1
                    achievementDao.updateAchievement(
                        ach.copy(currentProgress = completedBooks, isUnlocked = unlocked)
                    )
                }
            }
        }
    }
}
