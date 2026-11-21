package bg.uni_plovdiv.fmi.everquill.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import bg.uni_plovdiv.fmi.everquill.SettingsActivity
import bg.uni_plovdiv.fmi.everquill.data.entity.Achievement
import bg.uni_plovdiv.fmi.everquill.ui.screens.AchievementsScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.AchievementUnlockedDialog
import bg.uni_plovdiv.fmi.everquill.ui.screens.AddBookDialog
import bg.uni_plovdiv.fmi.everquill.ui.screens.BookDetailScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.HomeScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.LibraryScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.ReadingScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.RecordSessionDialog
import bg.uni_plovdiv.fmi.everquill.ui.screens.SearchBooksScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.StatisticsScreen
import bg.uni_plovdiv.fmi.everquill.util.generateBookShareText
import bg.uni_plovdiv.fmi.everquill.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Library : Screen("library", "Library", Icons.Default.List)
    data object Reading : Screen("reading", "Reading", Icons.Default.Create)
    data object Achievements : Screen("achievements", "Achievements", Icons.Default.Star)
    data object Statistics : Screen("statistics", "Stats", Icons.Default.Check)
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val books by viewModel.books.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    var currentScreenRoute by rememberSaveable { mutableStateOf(Screen.Home.route) }
    val currentScreen = when (currentScreenRoute) {
        Screen.Library.route -> Screen.Library
        Screen.Reading.route -> Screen.Reading
        Screen.Achievements.route -> Screen.Achievements
        Screen.Statistics.route -> Screen.Statistics
        else -> Screen.Home
    }

    var showSearchScreen by remember { mutableStateOf(false) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }

    var showAddBookDialog by remember { mutableStateOf(false) }
    var showRecordSessionDialog by remember { mutableStateOf(false) }

    var achievementPopupQueue by remember { mutableStateOf<List<Achievement>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.newlyUnlockedAchievements.collect { achievement ->
            achievementPopupQueue = achievementPopupQueue + achievement
        }
    }

    val currentAchievementPopup = achievementPopupQueue.firstOrNull()
    if (currentAchievementPopup != null) {
        AchievementUnlockedDialog(
            achievement = currentAchievementPopup,
            onContinue = {
                achievementPopupQueue = achievementPopupQueue.drop(1)
            }
        )
    }

    val screens = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Reading,
        Screen.Achievements,
        Screen.Statistics
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!showSearchScreen && selectedBookId == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentScreen == screen,
                            onClick = { currentScreenRoute = screen.route }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val selectedBook = books.find { book -> book.id == selectedBookId }
            when {
                selectedBook != null -> BookDetailScreen(
                    book = selectedBook,
                    onBackClick = { selectedBookId = null },
                    onUpdateStatus = { status ->
                        viewModel.updateBookDetails(selectedBook.id, selectedBook.title, selectedBook.author, selectedBook.totalPages, status)
                    },
                    onUpdateDetails = { title, author, totalPages, status ->
                        viewModel.updateBookDetails(selectedBook.id, title, author, totalPages, status)
                    },
                    onDeleteBook = {
                        viewModel.deleteBook(selectedBook.id)
                        selectedBookId = null
                    },
                    onShareClick = {
                        val shareText = generateBookShareText(selectedBook)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        val chooser = Intent.createChooser(intent, "Share book via")
                        context.startActivity(chooser)
                    }
                )
                showSearchScreen -> SearchBooksScreen(
                    existingBooks = books,
                    onBackClick = { showSearchScreen = false }
                )
                else -> {
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            userProfile = userProfile,
                            quests = quests,
                            books = books,
                            onNavigateToLibrary = { currentScreenRoute = Screen.Library.route },
                            onNavigateToReading = { currentScreenRoute = Screen.Reading.route },
                            onToggleQuest = { viewModel.toggleQuest(it) },
                            onNavigateToSettings = {
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                        Screen.Library -> {
                            LaunchedEffect(Unit) {
                                viewModel.markQuestCompleted("q2")
                            }
                            LibraryScreen(
                                books = books,
                                onAddBookClick = { showAddBookDialog = true },
                                onNavigateToSearch = { showSearchScreen = true },
                                onBookClick = { book -> selectedBookId = book.id },
                                onUpdateProgress = { bookId, page -> viewModel.updateBookProgress(bookId, page) }
                            )
                        }
                        Screen.Reading -> ReadingScreen(
                            userProfile = userProfile,
                            books = books,
                            onRecordSessionClick = { showRecordSessionDialog = true },
                            onNavigateToLibrary = { currentScreenRoute = Screen.Library.route }
                        )
                        Screen.Achievements -> AchievementsScreen(
                            achievements = achievements
                        )
                        Screen.Statistics -> StatisticsScreen(
                            userProfile = userProfile
                        )
                    }
                }
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
