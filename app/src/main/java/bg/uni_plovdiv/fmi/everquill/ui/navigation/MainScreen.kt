package bg.uni_plovdiv.fmi.everquill.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import bg.uni_plovdiv.fmi.everquill.ui.screens.AchievementsScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.AddBookDialog
import bg.uni_plovdiv.fmi.everquill.ui.screens.HomeScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.LibraryScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.ReadingScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.RecordSessionDialog
import bg.uni_plovdiv.fmi.everquill.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Library : Screen("library", "Library", Icons.Default.List)
    data object Reading : Screen("reading", "Reading", Icons.Default.Create)
    data object Achievements : Screen("achievements", "Achievements", Icons.Default.Star)
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val userProfile by viewModel.userProfile.collectAsState()
    val books by viewModel.books.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    var showAddBookDialog by remember { mutableStateOf(false) }
    var showRecordSessionDialog by remember { mutableStateOf(false) }

    val screens = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Reading,
        Screen.Achievements
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    userProfile = userProfile,
                    quests = quests,
                    books = books,
                    onNavigateToLibrary = { currentScreen = Screen.Library },
                    onNavigateToReading = { currentScreen = Screen.Reading },
                    onToggleQuest = { viewModel.toggleQuest(it) }
                )
                Screen.Library -> LibraryScreen(
                    books = books,
                    onAddBookClick = { showAddBookDialog = true },
                    onUpdateProgress = { bookId, page -> viewModel.updateBookProgress(bookId, page) }
                )
                Screen.Reading -> ReadingScreen(
                    userProfile = userProfile,
                    books = books,
                    onRecordSessionClick = { showRecordSessionDialog = true },
                    onNavigateToLibrary = { currentScreen = Screen.Library }
                )
                Screen.Achievements -> AchievementsScreen(
                    achievements = achievements
                )
            }
        }
    }

    if (showAddBookDialog) {
        AddBookDialog(
            onDismiss = { showAddBookDialog = false },
            onAddBook = { title, author, totalPages, currentPage, status ->
                viewModel.addBook(title, author, totalPages, currentPage, status)
            }
        )
    }

    if (showRecordSessionDialog) {
        RecordSessionDialog(
            books = books,
            onDismiss = { showRecordSessionDialog = false },
            onRecordSession = { bookId, pagesRead ->
                viewModel.recordReadingSession(bookId, pagesRead)
            }
        )
    }
}
