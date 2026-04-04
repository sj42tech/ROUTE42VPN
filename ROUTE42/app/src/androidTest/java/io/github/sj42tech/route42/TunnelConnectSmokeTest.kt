package io.github.sj42tech.route42

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun runsLinkHealthCheckFromProfileScreen() {
        importProfile(BaselineRealityLink)
        composeRule.onNodeWithText("Run Check").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                composeRule.onNodeWithText("Server TCP: reachable", substring = true).assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithText("Profile is ready to connect").assertIsDisplayed()
        composeRule.onNodeWithText("Server TCP: reachable", substring = true).assertIsDisplayed()
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

    @Test
    fun opensShareCodeScreenForImportedProfile() {
        importProfile(BaselineRealityLink)
        composeRule.onNodeWithText("Choose Routing Profile").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Create RU + Local Profile").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Preset: RU + Local").assertIsDisplayed() }.isSuccess
        }

        composeRule.onNodeWithText("Show Code").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithContentDescription("Share code for android-smoke").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("Scan this code in Route42 on another device to import the same connection.").assertIsDisplayed()
        composeRule.onNodeWithText("RU + Local").assertIsDisplayed()
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
                "encryption=none&security=reality&sni=www.debian.org&" +
                "fp=chrome&pbk=xAjc3oJaoU9psF_G2zQB5N-HV1ClgKQ1K8atsPPL6CY&sid=a1&type=tcp#android-smoke"

        const val SharedRealityLink =
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@5.39.219.74:443?" +
                "encryption=none&security=reality&sni=www.debian.org&" +
                "fp=chrome&pbk=xAjc3oJaoU9psF_G2zQB5N-HV1ClgKQ1K8atsPPL6CY&sid=a1&type=tcp#android-shared"

        const val BrokenRealityLink =
            "vless://775ed879-a162-45e3-b8af-c49f96eaede5@5.39.219.74:443?" +
                "encryption=none&security=reality&" +
                "fp=chrome&pbk=xAjc3oJaoU9psF_G2zQB5N-HV1ClgKQ1K8atsPPL6CY&sid=a1&type=tcp#android-broken"
    }
}
