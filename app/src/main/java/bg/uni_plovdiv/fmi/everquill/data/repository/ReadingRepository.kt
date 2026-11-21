package bg.uni_plovdiv.fmi.everquill.data.repository

import android.content.Context
import androidx.room.withTransaction
import bg.uni_plovdiv.fmi.everquill.data.database.AppDatabase
import bg.uni_plovdiv.fmi.everquill.data.database.entity.AchievementEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.BookEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.QuestEntity
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    var clockProvider: () -> Long = { System.currentTimeMillis() }

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

    private val _newlyUnlockedAchievements = MutableSharedFlow<Achievement>(replay = 0, extraBufferCapacity = 64)
    val newlyUnlockedAchievements: SharedFlow<Achievement> = _newlyUnlockedAchievements

    init {
        externalScope.launch {
            updateQuestsSync(suppressXp = true)
            updateAchievementsSync(suppressXp = true)
        }
    }

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
        val today = getCalendarDayEpoch(clockProvider())
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
        val todayEpoch = getCalendarDayEpoch(clockProvider())
        return sessions
            .filter { getCalendarDayEpoch(it.date) == todayEpoch }
            .sumOf { it.pagesRead }
    }

    fun calculateWeeklyOverview(sessions: List<ReadingSessionEntity>, dailyGoal: Int): WeeklyOverviewData {
        val cal = Calendar.getInstance()
        cal.timeInMillis = clockProvider()
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
                    .toSet()
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
            updateQuestsSync(suppressXp = false)
            updateAchievementsSync(suppressXp = false)
        }
    }

    suspend fun updateBookProgress(bookId: String, newCurrentPage: Int) {
        withContext(Dispatchers.IO) {
            val book = bookDao.getBookById(bookId) ?: return@withContext
            val clampedPage = newCurrentPage.coerceIn(0, book.totalPages)
            val newStatus = if (clampedPage >= book.totalPages && book.totalPages > 0) BookStatus.COMPLETED else book.status
            val updated = book.copy(currentPage = clampedPage, status = newStatus)
            bookDao.updateBook(updated)
            updateQuestsSync(suppressXp = false)
            updateAchievementsSync(suppressXp = false)
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
            updateQuestsSync(suppressXp = false)
            updateAchievementsSync(suppressXp = false)
        }
    }

    suspend fun deleteBook(bookId: String) {
        withContext(Dispatchers.IO) {
            val book = bookDao.getBookById(bookId) ?: return@withContext
            bookDao.deleteBook(book)
            updateQuestsSync(suppressXp = false)
            updateAchievementsSync(suppressXp = false)
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
                    date = clockProvider(),
                    xpEarned = xpEarned
                )
            )

            val profile = userProfileDao.getUserProfileSync() ?: UserProfileEntity(
                id = 1, name = "Reader", level = 1, currentXp = 0, maxXpForNextLevel = 100, pagesReadToday = 0, streakDays = 1, lastReadingDate = clockProvider(), dailyGoal = 20
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

            updateQuestsSync(suppressXp = false)
            updateAchievementsSync(suppressXp = false)
        }
    }

    suspend fun toggleQuest(questId: String) {
        database.withTransaction {
            val todayEpoch = getCalendarDayEpoch(clockProvider())
            val quest = questDao.getQuestById(questId) ?: return@withTransaction
            val newState = !quest.isCompleted

            var shouldAwardXp = false
            val newRewardClaimed = if (newState && !quest.rewardClaimed) {
                shouldAwardXp = true
                true
            } else {
                quest.rewardClaimed
            }

            val updatedQuest = quest.copy(
                isCompleted = newState,
                rewardClaimed = newRewardClaimed,
                lastCompletedDateEpoch = if (newState) todayEpoch else quest.lastCompletedDateEpoch,
                lastRewardedDateEpoch = if (newRewardClaimed) todayEpoch else quest.lastRewardedDateEpoch,
                currentProgress = if (newState) quest.targetPages else quest.currentProgress
            )
            questDao.insertOrUpdateQuest(updatedQuest)

            if (shouldAwardXp) {
                awardXpForQuest(quest.xpReward)
            }
        }
    }

    suspend fun markQuestCompleted(questId: String) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val todayEpoch = getCalendarDayEpoch(clockProvider())
                val quest = questDao.getQuestById(questId) ?: return@withTransaction
                if (questId == "q2") {
                    val alreadyRewarded = quest.lastRewardedDateEpoch == todayEpoch
                    val updated = quest.copy(
                        isCompleted = true,
                        currentProgress = quest.targetPages,
                        rewardClaimed = true,
                        lastCompletedDateEpoch = todayEpoch,
                        lastRewardedDateEpoch = todayEpoch
                    )
                    questDao.insertOrUpdateQuest(updated)

                    if (!alreadyRewarded) {
                        awardXpForQuest(quest.xpReward)
                    }
                }
            }
        }
    }

    private suspend fun awardXpForQuest(xpReward: Int) {
        val profile = userProfileDao.getUserProfileSync() ?: return
        var xp = profile.currentXp + xpReward
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

    private suspend fun updateQuestsSync(suppressXp: Boolean = false) {
        database.withTransaction {
            val todayEpoch = getCalendarDayEpoch(clockProvider())
            val sessions = readingSessionDao.getAllSessionsSync()
            val pagesToday = calculatePagesReadToday(sessions)

            // 1. Sync q1 ("Read 20 pages today")
            val q1 = questDao.getQuestById("q1") ?: QuestEntity(
                id = "q1",
                title = "Read 20 pages today",
                description = "Read at least 20 pages in your active books",
                targetPages = 20,
                currentProgress = 0,
                xpReward = 50,
                date = clockProvider(),
                isCompleted = false,
                rewardClaimed = false,
                lastCompletedDateEpoch = 0L,
                lastRewardedDateEpoch = 0L
            )

            val q1Completed = pagesToday >= q1.targetPages
            val q1LastCompletedEpoch = if (q1Completed) todayEpoch else if (q1.lastCompletedDateEpoch == todayEpoch) q1.lastCompletedDateEpoch else 0L

            var q1RewardClaimed = q1.rewardClaimed
            var q1LastRewardedEpoch = q1.lastRewardedDateEpoch
            if (q1LastRewardedEpoch < todayEpoch) {
                q1RewardClaimed = false
            }

            var awardXpForQ1 = false
            if (q1Completed && !q1RewardClaimed && q1LastRewardedEpoch < todayEpoch) {
                q1RewardClaimed = true
                q1LastRewardedEpoch = todayEpoch
                if (!suppressXp) {
                    awardXpForQ1 = true
                }
            }

            val updatedQ1 = q1.copy(
                currentProgress = pagesToday.coerceAtMost(q1.targetPages),
                isCompleted = q1Completed,
                rewardClaimed = q1RewardClaimed,
                lastCompletedDateEpoch = q1LastCompletedEpoch,
                lastRewardedDateEpoch = q1LastRewardedEpoch
            )
            questDao.insertOrUpdateQuest(updatedQ1)

            // 2. Sync q2 ("Open your library journal")
            val q2 = questDao.getQuestById("q2") ?: QuestEntity(
                id = "q2",
                title = "Open your library journal",
                description = "Explore your library collection",
                targetPages = 1,
                currentProgress = 0,
                xpReward = 20,
                date = clockProvider(),
                isCompleted = false,
                rewardClaimed = false,
                lastCompletedDateEpoch = 0L,
                lastRewardedDateEpoch = 0L
            )

            val q2Completed = q2.lastCompletedDateEpoch == todayEpoch
            var q2RewardClaimed = q2.rewardClaimed
            var q2LastRewardedEpoch = q2.lastRewardedDateEpoch
            if (q2LastRewardedEpoch < todayEpoch) {
                q2RewardClaimed = false
            }

            val updatedQ2 = q2.copy(
                currentProgress = if (q2Completed) 1 else 0,
                isCompleted = q2Completed,
                rewardClaimed = q2RewardClaimed,
                lastCompletedDateEpoch = if (q2Completed) q2.lastCompletedDateEpoch else 0L,
                lastRewardedDateEpoch = q2LastRewardedEpoch
            )
            questDao.insertOrUpdateQuest(updatedQ2)

            if (awardXpForQ1) {
                awardXpForQuest(q1.xpReward)
            }
        }
    }

    private suspend fun updateAchievementsSync(suppressXp: Boolean = false) {
        database.withTransaction {
            achievementDao.insertAll(getDefaultAchievements())

            val allBooks = bookDao.getAllBooksSync()
            val sessions = readingSessionDao.getAllSessionsSync()
            val totalPagesRead = sessions.sumOf { it.pagesRead }
            val completedBooks = allBooks.count { it.status == BookStatus.COMPLETED }
            val currentStreak = calculateCurrentStreak(sessions)
            val maxPagesInSession = sessions.maxOfOrNull { it.pagesRead } ?: 0

            val nightOwlCount = sessions.count { session ->
                val cal = Calendar.getInstance().apply { timeInMillis = session.date }
                cal.get(Calendar.HOUR_OF_DAY) >= 22
            }

            val readBookIds = sessions.map { it.bookId }.toSet()
            val readBooks = allBooks.filter { it.id in readBookIds || it.currentPage > 0 }
            val distinctGenres = mutableSetOf<String>()
            for (b in readBooks) {
                if (!b.genre.isNullOrBlank()) {
                    b.genre.split(",").map { it.trim().lowercase(Locale.getDefault()) }.filter { it.isNotBlank() }.forEach { distinctGenres.add(it) }
                }
            }
            val distinctGenresCount = distinctGenres.size
            val librarySize = allBooks.size

            val achievements = achievementDao.getAllAchievementsSync()
            var totalXpRewardToAdd = 0

            for (ach in achievements) {
                var progress = ach.currentProgress

                when (ach.id) {
                    "a1" -> progress = completedBooks
                    "a2" -> progress = totalPagesRead
                    "a13" -> progress = totalPagesRead
                    "a5" -> progress = totalPagesRead
                    "a3" -> progress = sessions.size
                    "a6" -> progress = maxPagesInSession
                    "a7" -> progress = nightOwlCount
                    "a8" -> progress = currentStreak
                    "a4" -> progress = currentStreak
                    "a9" -> progress = currentStreak
                    "a10" -> progress = librarySize
                    "a11" -> progress = distinctGenresCount
                    "a12" -> progress = librarySize
                }

                val unlocked = progress >= ach.requiredProgress
                var dateUnlocked = ach.dateUnlocked
                var rewardClaimed = ach.rewardClaimed

                if (unlocked && !rewardClaimed) {
                    rewardClaimed = true
                    dateUnlocked = ach.dateUnlocked ?: clockProvider()
                    if (!suppressXp) {
                        totalXpRewardToAdd += ach.xpReward
                        val updatedAch = ach.copy(
                            currentProgress = progress,
                            isUnlocked = unlocked,
                            dateUnlocked = dateUnlocked,
                            rewardClaimed = rewardClaimed
                        )
                        _newlyUnlockedAchievements.tryEmit(updatedAch.toDomain())
                    }
                }

                achievementDao.updateAchievement(
                    ach.copy(
                        currentProgress = progress,
                        isUnlocked = unlocked,
                        dateUnlocked = dateUnlocked,
                        rewardClaimed = rewardClaimed
                    )
                )
            }

            if (!suppressXp && totalXpRewardToAdd > 0) {
                val profile = userProfileDao.getUserProfileSync()
                if (profile != null) {
                    var xp = profile.currentXp + totalXpRewardToAdd
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
    }

    private fun getDefaultAchievements(): List<AchievementEntity> {
        return listOf(
            AchievementEntity("a1", "First Steps", "Complete your first book", "READING_MILESTONES", "COMMON", 25, 1, 0, false, null, false),
            AchievementEntity("a2", "Bookworm", "Read 100 total pages", "READING_MILESTONES", "RARE", 50, 100, 0, false, null, false),
            AchievementEntity("a13", "Page Turner", "Read 250 total pages", "READING_MILESTONES", "RARE", 50, 250, 0, false, null, false),
            AchievementEntity("a5", "Bibliophile", "Read 1,000 total pages", "READING_MILESTONES", "LEGENDARY", 250, 1000, 0, false, null, false),
            AchievementEntity("a3", "Getting Started", "Complete your first reading session", "READING_HABITS", "COMMON", 25, 1, 0, false, null, false),
            AchievementEntity("a6", "Marathon Reader", "Read at least 50 pages in a single reading session", "READING_HABITS", "RARE", 50, 50, 0, false, null, false),
            AchievementEntity("a7", "Night Owl", "Complete a reading session starting at or after 22:00", "READING_HABITS", "RARE", 50, 1, 0, false, null, false),
            AchievementEntity("a8", "Consistent Reader", "Maintain a 3-day reading streak", "DEDICATION", "COMMON", 25, 3, 0, false, null, false),
            AchievementEntity("a4", "Dedicated Reader", "Maintain a 7-day reading streak", "DEDICATION", "EPIC", 100, 7, 0, false, null, false),
            AchievementEntity("a9", "Committed Scholar", "Maintain a 14-day reading streak", "DEDICATION", "LEGENDARY", 250, 14, 0, false, null, false),
            AchievementEntity("a10", "Curator", "Add 5 books to the library", "COLLECTION_EXPLORATION", "RARE", 50, 5, 0, false, null, false),
            AchievementEntity("a11", "Genre Explorer", "Read books from at least 3 distinct genres", "COLLECTION_EXPLORATION", "EPIC", 100, 3, 0, false, null, false),
            AchievementEntity("a12", "Collector", "Add 20 books to the library", "COLLECTION_EXPLORATION", "LEGENDARY", 250, 20, 0, false, null, false)
        )
    }
}
