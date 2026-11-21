package bg.uni_plovdiv.fmi.everquill.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bg.uni_plovdiv.fmi.everquill.data.database.entity.BookEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.ReadingSessionEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.UserProfileEntity
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetBook() = runBlocking {
        val book = BookEntity("1", "Test Book", "Author", 300, 50, BookStatus.READING)
        db.bookDao().insertBook(book)
        val loaded = db.bookDao().getBookById("1")
        assertNotNull(loaded)
        assertEquals("Test Book", loaded?.title)
    }

    @Test
    fun updateBook() = runBlocking {
        val book = BookEntity("1", "Test Book", "Author", 300, 50, BookStatus.READING)
        db.bookDao().insertBook(book)
        
        val updated = book.copy(currentPage = 100)
        db.bookDao().updateBook(updated)
        
        val loaded = db.bookDao().getBookById("1")
        assertEquals(100, loaded?.currentPage)
    }

    @Test
    fun deleteBook() = runBlocking {
        val book = BookEntity("1", "Test Book", "Author", 300, 50, BookStatus.READING)
        db.bookDao().insertBook(book)
        
        db.bookDao().deleteBook(book)
        val loaded = db.bookDao().getBookById("1")
        assertNull(loaded)
    }

    @Test
    fun insertAndGetReadingSession() = runBlocking {
        val book = BookEntity("1", "Test Book", "Author", 300, 50, BookStatus.READING)
        db.bookDao().insertBook(book)

        val session = ReadingSessionEntity(bookId = "1", pagesRead = 20, startPage = 50, endPage = 70, xpEarned = 200)
        db.readingSessionDao().insertSession(session)

        val sessions = db.readingSessionDao().getSessionsForBook("1").first()
        assertEquals(1, sessions.size)
        assertEquals(20, sessions[0].pagesRead)
    }

    @Test
    fun deleteBookCascadesSessions() = runBlocking {
        val book1 = BookEntity("1", "Book 1", "Author", 300, 50, BookStatus.READING)
        val book2 = BookEntity("2", "Book 2", "Author", 300, 50, BookStatus.READING)
        db.bookDao().insertBook(book1)
        db.bookDao().insertBook(book2)

        val session1 = ReadingSessionEntity(bookId = "1", pagesRead = 20, startPage = 0, endPage = 20, xpEarned = 200)
        val session2 = ReadingSessionEntity(bookId = "1", pagesRead = 30, startPage = 20, endPage = 50, xpEarned = 300)
        val session3 = ReadingSessionEntity(bookId = "2", pagesRead = 10, startPage = 0, endPage = 10, xpEarned = 100)
        db.readingSessionDao().insertSession(session1)
        db.readingSessionDao().insertSession(session2)
        db.readingSessionDao().insertSession(session3)

        // Delete book1
        db.bookDao().deleteBook(book1)

        // Verify book1 sessions are deleted, but book2 sessions remain
        val sessions1 = db.readingSessionDao().getSessionsForBook("1").first()
        val sessions2 = db.readingSessionDao().getSessionsForBook("2").first()
        val allSessions = db.readingSessionDao().getAllSessionsSync()

        assertTrue(sessions1.isEmpty())
        assertEquals(1, sessions2.size)
        assertEquals(1, allSessions.size)
        assertEquals("2", allSessions[0].bookId)
    }

    @Test
    fun insertAndGetUserProfile() = runBlocking {
        val profile = UserProfileEntity(id = 1, name = "Scholar", level = 2, currentXp = 50, maxXpForNextLevel = 150, pagesReadToday = 10, streakDays = 4, lastReadingDate = System.currentTimeMillis())
        db.userProfileDao().insertUserProfile(profile)

        val loaded = db.userProfileDao().getUserProfileSync()
        assertNotNull(loaded)
        assertEquals(2, loaded?.level)
        assertEquals("Scholar", loaded?.name)
    }
}
