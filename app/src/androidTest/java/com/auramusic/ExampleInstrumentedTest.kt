package com.auramusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auramusic.ui.theme.AuraMusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appNameIsDisplayed() {
        composeTestRule.setContent {
            AuraMusicTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Aura Music",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Aura Music").assertIsDisplayed()
    }

    @Test
    fun themeAppliesCorrectly() {
        composeTestRule.setContent {
            AuraMusicTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Test",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()
    }
}
