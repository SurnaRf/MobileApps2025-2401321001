package bg.uni_plovdiv.fmi.everquill.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bg.uni_plovdiv.fmi.everquill.data.database.AppDatabase
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: ReadingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReadingRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAddAndRetrieveBook() = runBlocking {
        repository.addBook("Hobbit", "Tolkien", 310, 0, BookStatus.TO_READ)
        val books = repository.books.first { it.any { b -> b.title == "Hobbit" } }
        val added = books.find { b -> b.title == "Hobbit" }
        assertTrue(added != null)
        assertEquals("Tolkien", added?.author)
        assertEquals(310, added?.totalPages)
    }

    @Test
    fun testRecordReadingSessionAndXpGain() = runBlocking {
        repository.addBook("Hobbit", "Tolkien", 310, 0, BookStatus.READING)
        val books = repository.books.first { it.any { b -> b.title == "Hobbit" } }
        val book = books.first { b -> b.title == "Hobbit" }

        val initialProfile = repository.userProfile.first()
        val initialLevel = initialProfile.level
        val initialXp = initialProfile.currentXp

        repository.recordReadingSession(book.id, 15)

        val updatedBooks = repository.books.first { it.any { b -> b.currentPage == 15 } }
        val updatedBook = updatedBooks.first { b -> b.id == book.id }
        assertEquals(15, updatedBook.currentPage)

        // Initial 40 XP + 150 XP (15 pages * 10) + 50 XP (daily quest completion for 20 pages reached in later tests or 15 not reaching 20)
        val updatedProfile = repository.userProfile.first { it.currentXp != initialXp || it.level > initialLevel }
        assertEquals(1, updatedProfile.level)
        assertEquals(190, updatedProfile.currentXp) // 40 + 150 = 190 (q1 requires 20 pages, 15 is < 20)
    }

    @Test
    fun testPreventDuplicateXpForSameProgress() = runBlocking {
        repository.addBook("Hobbit", "Tolkien", 100, 0, BookStatus.READING)
        val book = repository.books.first { it.any { b -> b.title == "Hobbit" } }.first { b -> b.title == "Hobbit" }

        repository.recordReadingSession(book.id, 50)
        val xpAfterFirst = repository.userProfile.first { it.currentXp > 0 }.currentXp

        // Try logging the same pages again (currentPage is already 50)
        repository.recordReadingSession(book.id, 0)
        val xpAfterSecond = repository.userProfile.first().currentXp

        assertEquals(xpAfterFirst, xpAfterSecond)
    }

    @Test
    fun testQuestXpFarmingPrevention() = runBlocking {
        val initialProfile = repository.userProfile.first()
        val xpBefore = initialProfile.currentXp

        // Toggle quest q1 first check -> should award 50 XP
        repository.toggleQuest("q1")
        val profileAfterFirstCheck = repository.userProfile.first()
        val xpAfterFirstCheck = profileAfterFirstCheck.currentXp
        assertEquals(xpBefore + 50, xpAfterFirstCheck)

        // Uncheck quest q1 -> should NOT remove XP
        repository.toggleQuest("q1")
        val profileAfterUncheck = repository.userProfile.first()
        assertEquals(xpAfterFirstCheck, profileAfterUncheck.currentXp)

        // Check quest q1 again -> should NOT award XP a second time
        repository.toggleQuest("q1")
        val profileAfterSecondCheck = repository.userProfile.first()
        assertEquals(xpAfterFirstCheck, profileAfterSecondCheck.currentXp)
    }

    @Test
    fun testAchievementsStartupSeedingAndNoRetroactiveXp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo2 = ReadingRepository(context)
        val achievements = repo2.achievements.first()
        assertEquals(13, achievements.size)

        val profile = repo2.userProfile.first()
        assertEquals(40, profile.currentXp)
    }

    @Test
    fun testIdempotentAchievementSeeding() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo2 = ReadingRepository(context)
        val achievements1 = repo2.achievements.first()

        val repo3 = ReadingRepository(context)
        val achievements2 = repo3.achievements.first()

        assertEquals(achievements1.size, achievements2.size)
        assertEquals(13, achievements2.size)
    }

    @Test
    fun testBookwormAchievementUnlockThreshold() = runBlocking {
        repository.addBook("Book", "Author", 300, 0, BookStatus.READING)
        val book = repository.books.first { it.any { b -> b.title == "Book" } }.first { b -> b.title == "Book" }

        // Read 99 pages
        repository.recordReadingSession(book.id, 99)
        var achievements = repository.achievements.first()
        var bookworm = achievements.find { ach -> ach.id == "a2" }
        assertTrue(bookworm != null)
        assertEquals(false, bookworm?.isUnlocked)
        assertEquals(99, bookworm?.currentProgress)

        // Read 1 more page (total 100)
        repository.recordReadingSession(book.id, 1)
        achievements = repository.achievements.first()
        bookworm = achievements.find { ach -> ach.id == "a2" }
        assertTrue(bookworm?.isUnlocked == true)
        assertEquals(100, bookworm?.currentProgress)
    }

    @Test
    fun testPageTurnerAchievementUnlockThreshold() = runBlocking {
        repository.addBook("Book", "Author", 300, 0, BookStatus.READING)
        val book = repository.books.first { it.any { b -> b.title == "Book" } }.first { b -> b.title == "Book" }

        // Read 250 pages
        repository.recordReadingSession(book.id, 250)
        val achievements = repository.achievements.first()
        val pageTurner = achievements.find { ach -> ach.id == "a13" }
        assertTrue(pageTurner != null)
        assertTrue(pageTurner?.isUnlocked == true)
        assertEquals(250, pageTurner?.currentProgress)
    }

    @Test
    fun testNewlyUnlockedAchievementsEmission() = runBlocking {
        val emitted = mutableListOf<bg.uni_plovdiv.fmi.everquill.data.entity.Achievement>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val job = scope.launch {
            repository.newlyUnlockedAchievements.collect { emitted.add(it) }
        }

        repository.addBook("Book 1", "Author", 100, 99, BookStatus.READING)
        val book = repository.books.first { it.any { b -> b.title == "Book 1" } }.first { b -> b.title == "Book 1" }

        repository.recordReadingSession(book.id, 1)
        kotlinx.coroutines.delay(100)

        assertTrue(emitted.isNotEmpty())
        job.cancel()
    }

    @Test
    fun testDailyQuestsScenarios() = runBlocking {
        // 1. New user starts with both quests incomplete
        val questsInitial = repository.quests.first()
        val q1 = questsInitial.find { it.id == "q1" }!!
        val q2 = questsInitial.find { it.id == "q2" }!!
        assertEquals(false, q1.isCompleted)
        assertEquals(false, q2.isCompleted)

        repository.addBook("Book", "Author", 100, 0, BookStatus.READING)
        val book = repository.books.first { it.isNotEmpty() }.first()

        // 2. Recording 10 pages does not complete q1
        repository.recordReadingSession(book.id, 10)
        var questsAfter10 = repository.quests.first()
        assertEquals(false, questsAfter10.find { it.id == "q1" }!!.isCompleted)

        // 3. Recording another 10 pages on the same day completes q1
        repository.recordReadingSession(book.id, 10)
        var questsAfter20 = repository.quests.first()
        val q1After20 = questsAfter20.find { it.id == "q1" }!!
        assertEquals(true, q1After20.isCompleted)

        // 7. Reopening application / syncing on same day does not award XP again
        val xpAfterFirstCompletion = repository.userProfile.first().currentXp
        repository.addBook("Book 2", "Author", 50, 0, BookStatus.TO_READ)
        val xpAfterSync = repository.userProfile.first().currentXp
        assertEquals(xpAfterFirstCompletion, xpAfterSync)

        // 5. Opening Library screen completes q2
        repository.markQuestCompleted("q2")
        val questsAfterLibrary = repository.quests.first()
        assertEquals(true, questsAfterLibrary.find { it.id == "q2" }!!.isCompleted)

        // 8. Advancing to a new calendar day resets both quests
        val currentDayEpoch = repository.getCalendarDayEpoch(System.currentTimeMillis())
        repository.clockProvider = { (currentDayEpoch + 1) * 86400000L + 1000L }

        repository.addBook("Book 3", "Author", 50, 0, BookStatus.TO_READ)
        val questsNewDay = repository.quests.first()
        assertEquals(false, questsNewDay.find { it.id == "q1" }!!.isCompleted)
        assertEquals(false, questsNewDay.find { it.id == "q2" }!!.isCompleted)

        // 9 & 10. Completing q1 and q2 again on next day awards reward exactly once
        repository.recordReadingSession(book.id, 20)
        val xpBeforeQ1NextDay = repository.userProfile.first().currentXp
        repository.recordReadingSession(book.id, 0)
        val xpAfterQ1NextDay = repository.userProfile.first().currentXp
        assertEquals(50, xpAfterQ1NextDay - xpBeforeQ1NextDay)

        repository.markQuestCompleted("q2")
        val xpBeforeQ2NextDay = repository.userProfile.first().currentXp
        repository.markQuestCompleted("q2")
        val xpAfterQ2NextDay = repository.userProfile.first().currentXp
        assertEquals(0, xpAfterQ2NextDay - xpBeforeQ2NextDay)
    }
}
