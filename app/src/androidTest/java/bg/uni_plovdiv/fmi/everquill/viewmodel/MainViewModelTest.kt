package bg.uni_plovdiv.fmi.everquill.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainViewModelTest {

    @Test
    fun testViewModelInitialStateAndAddBook() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        viewModel.addBook("Test ViewModel Book", "Author X", 200, 10, BookStatus.READING)
        val updatedBooks = viewModel.books.first { it.any { b -> b.title == "Test ViewModel Book" } }
        val added = updatedBooks.find { it.title == "Test ViewModel Book" }
        assertNotNull(added)
        assertEquals(200, added?.totalPages)
        assertEquals(10, added?.currentPage)
    }
}
