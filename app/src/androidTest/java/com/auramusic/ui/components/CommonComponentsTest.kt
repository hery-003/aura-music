package com.auramusic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.auramusic.ui.theme.AuraMusicTheme
import org.junit.Rule
import org.junit.Test

class CommonComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun neonDividerIsDisplayed() {
        composeTestRule.setContent {
            AuraMusicTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Above")
                    NeonDivider()
                    Text("Below")
                }
            }
        }
        composeTestRule.onNodeWithText("Above").assertIsDisplayed()
        composeTestRule.onNodeWithText("Below").assertIsDisplayed()
    }

    @Test
    fun glassmorphismCardRendersContent() {
        composeTestRule.setContent {
            AuraMusicTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    GlassmorphismCard(
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("Glass Content")
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Glass Content").assertIsDisplayed()
    }
}
