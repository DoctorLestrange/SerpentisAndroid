package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Serpentis", appName)
    }

    @Test
    fun `navigate across arcade tabs and launch arena`() {
        // Scroll to and verify Home screen poison toggle & launch button are displayed
        composeTestRule.onNodeWithTag("home_poison_toggle")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("launch_arena_button")
            .performScrollTo()
            .assertIsDisplayed()

        // Navigate to Workshop tab and select a skin
        composeTestRule.onNodeWithTag("nav_tab_workshop").performClick()
        composeTestRule.onNodeWithTag("skin_card_solar_flare")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // Navigate to Settings tab and toggle Full-Area Play Grid & Poison Hazard
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.onNodeWithTag("toggle_full_area_grid")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("toggle_poison_hazard")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("control_scheme_split_buttons")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // Navigate to Arena tab and verify board canvas and HUD full-area button are displayed
        composeTestRule.onNodeWithTag("nav_tab_arena").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("snake_board_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hud_full_area_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("arena_swipe_region").assertIsDisplayed()
    }

    @Test
    fun `switch app themes, custom accents, device colors, and minimalist light or dark play boards`() {
        // Open Workshop and test App Interface Themes (Soft Light, Midnight Cyber, Dark Graphite)
        composeTestRule.onNodeWithTag("nav_tab_workshop").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_theme_soft_light")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("accent_card_cobalt_teal")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("workshop_dynamic_colors_toggle")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        // Switch between Soft Paper (off-white board) and Graphite Matte (dark gray board)
        composeTestRule.onNodeWithTag("theme_card_paper_minimal")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("theme_card_graphite_minimal")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        // Open Settings and verify quick theme & dynamic color controls
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_theme_midnight_cyber")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_dynamic_colors")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }
}
