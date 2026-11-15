package bg.uni_plovdiv.fmi.everquill.data.repository

import bg.uni_plovdiv.fmi.everquill.data.entity.Achievement
import bg.uni_plovdiv.fmi.everquill.data.entity.Book
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import bg.uni_plovdiv.fmi.everquill.data.entity.Quest
import bg.uni_plovdiv.fmi.everquill.data.entity.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ReadingRepository {

    private val _userProfile = MutableStateFlow(UserProfile(name = "Reader", level = 1, currentXp = 40, maxXpForNextLevel = 100, pagesReadToday = 15, streakDays = 3))
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _books = MutableStateFlow(
        listOf(
            Book(
                id = "1",
                title = "The Fellowship of the Ring",
                author = "J.R.R. Tolkien",
                totalPages = 423,
                currentPage = 120,
                status = BookStatus.READING
            ),
            Book(
                id = "2",
                title = "Dune",
                author = "Frank Herbert",
                totalPages = 412,
                currentPage = 0,
                status = BookStatus.TO_READ
            )
        )
    )
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _quests = MutableStateFlow(
        listOf(
            Quest(
                id = "q1",
                title = "Read 20 pages today",
                rewardXp = 50,
                isCompleted = false
            ),
            Quest(
                id = "q2",
                title = "Open your library journal",
                rewardXp = 20,
                isCompleted = true
            )
        )
    )
    val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _achievements = MutableStateFlow(
        listOf(
            Achievement(
                id = "a1",
                title = "First Steps",
                description = "Read your first page",
                isUnlocked = true,
                progress = 1.0f,
                progressText = "Unlocked"
            ),
            Achievement(
                id = "a2",
                title = "Bookworm",
                description = "Read 100 pages",
                isUnlocked = true,
                progress = 1.0f,
                progressText = "120 / 100 pages"
            ),
            Achievement(
                id = "a3",
                title = "Getting Started",
                description = "Complete your first book",
                isUnlocked = false,
                progress = 0.0f,
                progressText = "0 / 1 books"
            ),
            Achievement(
                id = "a4",
                title = "Dedicated Reader",
                description = "Read for 7 consecutive days",
                isUnlocked = false,
                progress = 3f / 7f,
                progressText = "3 / 7 days"
            )
        )
    )
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    fun addBook(title: String, author: String, totalPages: Int, currentPage: Int, status: BookStatus) {
        val newBook = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            totalPages = totalPages,
            currentPage = currentPage.coerceIn(0, totalPages),
            status = status
        )
        _books.update { current -> listOf(newBook) + current }
    }

    fun updateBookProgress(bookId: String, newCurrentPage: Int) {
        _books.update { current ->
            current.map { book ->
                if (book.id == bookId) {
                    val clampedPage = newCurrentPage.coerceIn(0, book.totalPages)
                    val newStatus = if (clampedPage >= book.totalPages && book.totalPages > 0) BookStatus.COMPLETED else book.status
                    book.copy(currentPage = clampedPage, status = newStatus)
                } else {
                    book
                }
            }
        }
    }

    fun recordReadingSession(bookId: String, pagesRead: Int) {
        if (pagesRead <= 0) return
        
        // Update book progress
        var actualPagesRead = pagesRead
        _books.update { current ->
            current.map { book ->
                if (book.id == bookId) {
                    val newPage = (book.currentPage + pagesRead).coerceAtMost(book.totalPages)
                    actualPagesRead = newPage - book.currentPage
                    val newStatus = if (newPage >= book.totalPages && book.totalPages > 0) BookStatus.COMPLETED else BookStatus.READING
                    book.copy(currentPage = newPage, status = newStatus)
                } else {
                    book
                }
            }
        }

        if (actualPagesRead <= 0) return

        // Add XP (10 XP per page read)
        val xpGained = actualPagesRead * 10
        _userProfile.update { profile ->
            var newXp = profile.currentXp + xpGained
            var newLevel = profile.level
            var maxLevelXp = profile.maxXpForNextLevel
            
            while (newXp >= maxLevelXp) {
                newXp -= maxLevelXp
                newLevel += 1
                maxLevelXp += 100 // Scale next level requirement
            }

            profile.copy(
                level = newLevel,
                currentXp = newXp,
                maxXpForNextLevel = maxLevelXp,
                pagesReadToday = profile.pagesReadToday + actualPagesRead
            )
        }

        // Check quest completion if daily pages >= 20
        _quests.update { quests ->
            quests.map { quest ->
                if (quest.id == "q1" && !quest.isCompleted) {
                    val currentPagesToday = _userProfile.value.pagesReadToday
                    if (currentPagesToday >= 20) {
                        // Grant bonus XP for quest
                        _userProfile.update { p ->
                            var xp = p.currentXp + quest.rewardXp
                            var lvl = p.level
                            var maxXp = p.maxXpForNextLevel
                            if (xp >= maxXp) {
                                xp -= maxXp
                                lvl += 1
                                maxXp += 100
                            }
                            p.copy(level = lvl, currentXp = xp, maxXpForNextLevel = maxXp)
                        }
                        quest.copy(isCompleted = true)
                    } else {
                        quest
                    }
                } else {
                    quest
                }
            }
        }

        // Update achievements progress
        _achievements.update { achievements ->
            achievements.map { ach ->
                when (ach.id) {
                    "a2" -> { // Bookworm (100 pages total across books)
                        val totalPagesRead = _books.value.sumOf { it.currentPage }
                        val prog = (totalPagesRead.toFloat() / 100f).coerceIn(0f, 1f)
                        ach.copy(
                            isUnlocked = totalPagesRead >= 100,
                            progress = prog,
                            progressText = "${totalPagesRead} / 100 pages"
                        )
                    }
                    "a3" -> { // Getting Started (Complete 1 book)
                        val completedBooks = _books.value.count { it.status == BookStatus.COMPLETED }
                        val prog = (completedBooks.toFloat() / 1f).coerceIn(0f, 1f)
                        ach.copy(
                            isUnlocked = completedBooks >= 1,
                            progress = prog,
                            progressText = "$completedBooks / 1 books"
                        )
                    }
                    else -> ach
                }
            }
        }
    }

    fun toggleQuest(questId: String) {
        _quests.update { quests ->
            quests.map { quest ->
                if (quest.id == questId) {
                    val newState = !quest.isCompleted
                    if (newState) {
                        _userProfile.update { p ->
                            var xp = p.currentXp + quest.rewardXp
                            var lvl = p.level
                            var maxXp = p.maxXpForNextLevel
                            if (xp >= maxXp) {
                                xp -= maxXp
                                lvl += 1
                                maxXp += 100
                            }
                            p.copy(level = lvl, currentXp = xp, maxXpForNextLevel = maxXp)
                        }
                    }
                    quest.copy(isCompleted = newState)
                } else {
                    quest
                }
            }
        }
    }
}
