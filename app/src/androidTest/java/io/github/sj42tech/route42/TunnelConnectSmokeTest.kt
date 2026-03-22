package io.github.sj42tech.route42

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithText("Import").assertIsDisplayed().performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(BaselineRealityLink)
        composeRule.onNodeWithText("Save Profile").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Connect").assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithText("Connect").performClick()

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
    }
}
