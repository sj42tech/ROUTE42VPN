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
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class TunnelConnectSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun importsRealityProfileAndReachesRunningState() {
        importProfile(requireLiveBaselineLink())
        composeRule.onNodeWithText("Connect").performClick()

        assertVpnConnected()
    }

    @Test
    fun runsLinkHealthCheckFromProfileScreen() {
        importProfile(requireLiveBaselineLink())
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
        composeRule.onNode(hasSetTextAction()).performTextInput(ExampleBrokenRealityLink)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Reality link is missing SNI").assertIsDisplayed() }.isSuccess
        }
        composeRule.onAllNodesWithText("Save Profile").assertCountEquals(0)
    }

    @Test
    fun sharesRuLocalPresetAcrossTwoProfilesAndConnects() {
        importProfile(requireLiveBaselineLink())
        composeRule.onNodeWithText("Choose Routing Profile").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Create RU + Local Profile").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Preset: RU + Local").assertIsDisplayed() }.isSuccess
        }

        composeRule.onNodeWithText("Back").performClick()
        importProfile(requireLiveSharedLink())
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
        importProfile(ExampleRealityLink)
        composeRule.onNodeWithText("Choose Routing Profile").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Create RU + Local Profile").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Preset: RU + Local").assertIsDisplayed() }.isSuccess
        }

        composeRule.onNodeWithText("Show Code").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithContentDescription("Share code for example-smoke").assertIsDisplayed()
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

    private fun requireLiveBaselineLink(): String =
        requireInstrumentationArgument("route42.live_link")

    private fun requireLiveSharedLink(): String =
        readInstrumentationArgument("route42.shared_live_link")
            ?.takeUnless(String::isBlank)
            ?: withProfileName(requireLiveBaselineLink(), "android-shared")

    private fun requireInstrumentationArgument(name: String): String {
        val value = readInstrumentationArgument(name)
        assumeTrue("Missing instrumentation argument: $name", !value.isNullOrBlank())
        return checkNotNull(value)
    }

    private fun readInstrumentationArgument(name: String): String? =
        InstrumentationRegistry.getArguments().getString(name)?.trim()

    private fun withProfileName(link: String, profileName: String): String =
        "${link.substringBefore('#')}#$profileName"

    private companion object {
        const val ExampleRealityLink =
            "vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?" +
                "encryption=none&security=reality&sni=cdn.example&" +
                "fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&type=tcp#example-smoke"

        const val ExampleBrokenRealityLink =
            "vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?" +
                "encryption=none&security=reality&" +
                "fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&type=tcp#example-broken"
    }
}
