package io.github.sj42tech.route42

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TunnelConnectSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun importsRealityProfileAndReachesRunningState() {
        importProfile(BaselineRealityLink)
        composeRule.onNodeWithText("Connect").performClick()

        assertVpnConnected()
    }

    @Test
    fun rejectsBrokenRealityLinkOnImportScreen() {
        composeRule.onNodeWithText("Import").assertIsDisplayed().performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(BrokenRealityLink)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Reality link is missing SNI").assertIsDisplayed() }.isSuccess
        }
        composeRule.onAllNodesWithText("Save Profile").assertCountEquals(0)
    }

    @Test
    fun sharesRuLocalPresetAcrossTwoProfilesAndConnects() {
        importProfile(BaselineRealityLink)
        composeRule.onNodeWithText("Choose Routing Profile").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Create RU + Local Profile").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Preset: RU + Local").assertIsDisplayed() }.isSuccess
        }

        composeRule.onNodeWithText("Back").performClick()
        importProfile(SharedRealityLink)
        composeRule.onNodeWithText("Choose Routing Profile").assertIsDisplayed().performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Rule (RU + Local)"))
        composeRule.onNodeWithText("Use Rule", substring = true).assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Shared with", substring = true).assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithText("Preset: RU + Local").assertIsDisplayed()
        composeRule.onNodeWithText("Connect").performClick()

        assertVpnConnected()
    }

    private fun importProfile(link: String) {
        composeRule.onNodeWithText("Import").assertIsDisplayed().performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(link)
        composeRule.onNodeWithText("Save Profile").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Connect").assertIsDisplayed() }.isSuccess
        }
    }

    private fun assertVpnConnected() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.findObject(By.text("OK")), 5_000)?.click()
        device.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 20_000) {
            device.executeShellCommand("dumpsys connectivity")
                .contains("VPN CONNECTED extra: VPN:io.github.sj42tech.route42")
        }

        val connectivityDump = device.executeShellCommand("dumpsys connectivity")
        assertTrue(
            connectivityDump,
            connectivityDump.contains("VPN CONNECTED extra: VPN:io.github.sj42tech.route42"),
        )
    }

    private companion object {
        const val BaselineRealityLink =
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@5.39.219.74:443?" +
                "encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.microsoft.com&" +
                "fp=chrome&pbk=fivTvehL9FxvXGc9TmVPtOJa2baWSl8DkyAvoTLb0Q8&sid=f3aa&spx=%2F&type=tcp#android-smoke"

        const val SharedRealityLink =
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@5.39.219.74:443?" +
                "encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.microsoft.com&" +
                "fp=chrome&pbk=fivTvehL9FxvXGc9TmVPtOJa2baWSl8DkyAvoTLb0Q8&sid=f3aa&spx=%2F&type=tcp#android-shared"

        const val BrokenRealityLink =
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@5.39.219.74:443?" +
                "encryption=none&flow=xtls-rprx-vision&security=reality&" +
                "fp=chrome&pbk=fivTvehL9FxvXGc9TmVPtOJa2baWSl8DkyAvoTLb0Q8&sid=f3aa&spx=%2F&type=tcp#android-broken"
    }
}
