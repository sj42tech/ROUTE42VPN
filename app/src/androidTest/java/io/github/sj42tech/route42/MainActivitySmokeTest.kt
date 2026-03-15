package io.github.sj42tech.route42

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensImportScreenFromMainToolbar() {
        composeRule.onNodeWithText("Route42").assertIsDisplayed()
        composeRule.onNodeWithText("Import").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Import Link").assertIsDisplayed()
    }
}
