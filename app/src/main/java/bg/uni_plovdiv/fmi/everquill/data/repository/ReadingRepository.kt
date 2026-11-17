package bg.uni_plovdiv.fmi.everquill.data.repository

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DayReadingStat(
    val dayName: String,
    val dateEpoch: Long,
    val pagesRead: Int,
    val goalReached: Boolean
)

data class WeeklyOverviewData(
    val days: List<DayReadingStat>,
    val goalCompletedDaysCount: Int,
    val totalPagesThisWeek: Int
)

data class MonthlyStat(
    val monthKey: String,
    val monthDisplayName: String,
    val pagesRead: Int
)

data class ReadingInsights(
    val totalBooksCompleted: Int,
    val totalPagesRead: Int,
    val totalSessions: Int,
    val activeReadingDays: Int,
    val averagePagesPerActiveDay: Float,
    val averagePagesPerSession: Float,
    val goalReachedDaysCount: Int,
    val mostActiveDayOfWeek: String?,
    val mostActiveMonth: String?,
    val genreDistribution: Map<String, Int>
)

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

    val allSessions: StateFlow<List<ReadingSessionEntity>> = readingSessionDao.getAllSessions()
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSessionsForBook(bookId: String): Flow<List<ReadingSessionEntity>> {
        return readingSessionDao.getSessionsForBook(bookId)
    }

    fun getCalendarDayEpoch(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / 86400000L
    }

    fun calculateCurrentStreak(sessions: List<ReadingSessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        val readingDays = sessions.map { getCalendarDayEpoch(it.date) }.toSet()
        val today = getCalendarDayEpoch(System.currentTimeMillis())
        val yesterday = today - 1

        if (!readingDays.contains(today) && !readingDays.contains(yesterday)) {
            return 0
        }

        var currentDay = if (readingDays.contains(today)) today else yesterday
        var streak = 0
        while (readingDays.contains(currentDay)) {
            streak++
            currentDay--
        }
        return streak
    }

    fun calculateLongestStreak(sessions: List<ReadingSessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        val readingDays = sessions.map { getCalendarDayEpoch(it.date) }.toSet().sorted()
        if (readingDays.isEmpty()) return 0

        var maxStreak = 0
        var currentStreak = 0
        var expectedDay = -1L

        for (day in readingDays) {
            if (expectedDay == -1L || day == expectedDay + 1) {
                currentStreak++
            } else if (day > expectedDay + 1) {
                currentStreak = 1
            }
            expectedDay = day
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        }
        return maxStreak
    }

    fun calculatePagesReadToday(sessions: List<ReadingSessionEntity>): Int {
        val todayEpoch = getCalendarDayEpoch(System.currentTimeMillis())
        return sessions
            .filter { getCalendarDayEpoch(it.date) == todayEpoch }
            .sumOf { it.pagesRead }
    }

    fun calculateWeeklyOverview(sessions: List<ReadingSessionEntity>, dailyGoal: Int): WeeklyOverviewData {
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val stats = mutableListOf<DayReadingStat>()
        var goalCompletedCount = 0
        var totalPages = 0

        val sessionMap = sessions.groupBy { getCalendarDayEpoch(it.date) }

        for (i in 0..6) {
            val dayEpoch = cal.timeInMillis / 86400000L
            val dayPages = sessionMap[dayEpoch]?.sumOf { it.pagesRead } ?: 0
            val goalReached = dayPages >= dailyGoal
            if (goalReached) goalCompletedCount++
            totalPages += dayPages

            stats.add(
                DayReadingStat(
                    dayName = dayNames[i],
                    dateEpoch = dayEpoch,
                    pagesRead = dayPages,
                    goalReached = goalReached
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return WeeklyOverviewData(
            days = stats,
            goalCompletedDaysCount = goalCompletedCount,
            totalPagesThisWeek = totalPages
        )
    }

    fun calculateMonthlyStats(sessions: List<ReadingSessionEntity>): List<MonthlyStat> {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val grouped = sessions.groupBy {
            dateFormat.format(Date(it.date))
        }
        return grouped.entries.map { (key, list) ->
            val date = dateFormat.parse(key) ?: Date()
            MonthlyStat(
                monthKey = key,
                monthDisplayName = displayFormat.format(date),
                pagesRead = list.sumOf { it.pagesRead }
            )
        }.sortedBy { it.monthKey }
    }

    fun calculateGenreDistribution(books: List<Book>): Map<String, Int> {
        val genreCounts = mutableMapOf<String, Int>()
        for (book in books) {
            if (!book.genre.isNullOrBlank()) {
                val genres = book.genre.split(",")
                    .map { it.trim().lowercase(Locale.getDefault()).replaceFirstChar { c -> c.uppercase() } }
                    .filter { it.isNotBlank() }
                    .toSet() // each book counts at most once per normalized genre
                for (genre in genres) {
                    genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
                }
            }
        }
        return genreCounts.entries.sortedByDescending { it.value }.associate { it.key to it.value }
    }

    fun calculateGoalReachedDaysCount(sessions: List<ReadingSessionEntity>, dailyGoal: Int): Int {
        if (sessions.isEmpty()) return 0
        val sessionMap = sessions.groupBy { getCalendarDayEpoch(it.date) }
        return sessionMap.values.count { daySessions ->
            daySessions.sumOf { it.pagesRead } >= dailyGoal
        }
    }

    fun calculateMostActiveDayOfWeek(sessions: List<ReadingSessionEntity>): String? {
        if (sessions.isEmpty()) return null
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val daySums = mutableMapOf<Int, Int>()
        val cal = Calendar.getInstance()
        for (session in sessions) {
            cal.timeInMillis = session.date
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            daySums[dayOfWeek] = (daySums[dayOfWeek] ?: 0) + session.pagesRead
        }
        val bestDay = daySums.maxByOrNull { it.value }?.key ?: return null
        return dayNames[bestDay - 1]
    }

    fun calculateReadingInsights(sessions: List<ReadingSessionEntity>, books: List<Book>, dailyGoal: Int): ReadingInsights {
        val totalBooksCompleted = books.count { it.status == BookStatus.COMPLETED }
        val totalPagesRead = sessions.sumOf { it.pagesRead }
        val totalSessions = sessions.size
        val activeDaysSet = sessions.map { getCalendarDayEpoch(it.date) }.toSet()
        val activeReadingDays = activeDaysSet.size

        val avgPagesPerActiveDay = if (activeReadingDays > 0) totalPagesRead.toFloat() / activeReadingDays.toFloat() else 0f
        val avgPagesPerSession = if (totalSessions > 0) totalPagesRead.toFloat() / totalSessions.toFloat() else 0f
        val goalReachedDaysCount = calculateGoalReachedDaysCount(sessions, dailyGoal)
        val mostActiveDayOfWeek = calculateMostActiveDayOfWeek(sessions)

        val monthlyStats = calculateMonthlyStats(sessions)
        val mostActiveMonth = monthlyStats.maxByOrNull { it.pagesRead }?.monthDisplayName
        val genreDistribution = calculateGenreDistribution(books)

        return ReadingInsights(
            totalBooksCompleted = totalBooksCompleted,
            totalPagesRead = totalPagesRead,
            totalSessions = totalSessions,
            activeReadingDays = activeReadingDays,
            averagePagesPerActiveDay = avgPagesPerActiveDay,
            averagePagesPerSession = avgPagesPerSession,
            goalReachedDaysCount = goalReachedDaysCount,
            mostActiveDayOfWeek = mostActiveDayOfWeek,
            mostActiveMonth = mostActiveMonth,
            genreDistribution = genreDistribution
        )
    }

    suspend fun updateDailyGoal(newGoal: Int) {
        withContext(Dispatchers.IO) {
            val profile = userProfileDao.getUserProfileSync() ?: return@withContext
            userProfileDao.updateUserProfile(profile.copy(dailyGoal = newGoal.coerceAtLeast(1)))
        }
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

            val profile = userProfileDao.getUserProfileSync() ?: UserProfileEntity(
                id = 1, name = "Reader", level = 1, currentXp = 0, maxXpForNextLevel = 100, pagesReadToday = 0, streakDays = 1, lastReadingDate = System.currentTimeMillis(), dailyGoal = 20
            )

            val sessions = readingSessionDao.getAllSessionsSync()
            val newPagesToday = calculatePagesReadToday(sessions)
            val currentStreak = calculateCurrentStreak(sessions)

            var newXp = profile.currentXp + xpEarned
            var newLevel = profile.level
            var maxLevelXp = profile.maxXpForNextLevel

            while (newXp >= maxLevelXp) {
                newXp -= maxLevelXp
                newLevel += 1
                maxLevelXp += 100
            }

            userProfileDao.updateUserProfile(
                profile.copy(
                    level = newLevel,
                    currentXp = newXp,
                    maxXpForNextLevel = maxLevelXp,
                    pagesReadToday = newPagesToday,
                    streakDays = currentStreak
                )
            )

            // Update quest q1 ("Read 20 pages today")
            val quest1 = questDao.getQuestById("q1")
            if (quest1 != null && !quest1.isCompleted) {
                val updatedProgress = newPagesToday.coerceAtMost(quest1.targetPages)
                val isCompleted = updatedProgress >= quest1.targetPages
                questDao.insertOrUpdateQuest(
                    quest1.copy(currentProgress = updatedProgress, isCompleted = isCompleted)
                )
                if (isCompleted) {
                    var xp = profile.currentXp + quest1.xpReward
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

        val sessions = readingSessionDao.getAllSessionsSync()
        val currentStreak = calculateCurrentStreak(sessions)

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
                "a4" -> { // Dedicated Reader (7 consecutive days)
                    val unlocked = currentStreak >= 7
                    achievementDao.updateAchievement(
                        ach.copy(currentProgress = currentStreak, isUnlocked = unlocked)
                    )
                }
            }
        }
    }
}
