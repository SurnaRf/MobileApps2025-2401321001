package bg.uni_plovdiv.fmi.everquill.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import bg.uni_plovdiv.fmi.everquill.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testBottomNavigationAndAddBookFlow() {
        // Verify bottom navigation items are visible
        composeTestRule.onNodeWithText("Library").assertExists()
        composeTestRule.onNodeWithText("Reading").assertExists()
        composeTestRule.onNodeWithText("Achievements").assertExists()

        // Navigate to Library screen
        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.onNodeWithText("My Library").assertExists()

        // Open Add Book dialog
        composeTestRule.onNodeWithContentDescription("Add Book").performClick()
        composeTestRule.onNodeWithText("Add New Book").assertExists()

        // Enter book title, author, and total pages using composeTestRule.onAllNodes()
        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        textFields[0].performTextInput("Compose Mastery")
        textFields[1].performTextInput("Jetpack Expert")
        textFields[2].performTextInput("250")

        // Submit form
        composeTestRule.onNodeWithText("Add Book").performClick()

        // Verify the new book appears in the library
        composeTestRule.onNodeWithText("Compose Mastery").assertExists()
        composeTestRule.onNodeWithText("Jetpack Expert", substring = true).assertExists()
    }
}
