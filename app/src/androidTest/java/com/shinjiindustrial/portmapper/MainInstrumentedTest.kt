package com.shinjiindustrial.portmapper

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shinjiindustrial.portmapper.client.IUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.client.UpnpClientModule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule


@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(UpnpClientModule::class)
class MainInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val upnpClient: IUpnpClient = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest, RuleSet.Demo))

    @Rule @JvmField val locale = LocaleTestRule()

    @Before fun strategy() {
        // Usually the default already, but explicit is fine:
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
    }

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Before
    fun beforeClean() {
        CleanStatusBar.enableWithDefaults()
    }

    @After
    fun afterClean() {
        CleanStatusBar.disable()
    }

    @Test
    fun tapButton_showsConfirmationText() {
        // The Activity is launched automatically by createAndroidComposeRule
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("createRuleFab").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        Screengrab.screenshot("home")

        compose.onNodeWithTag("moreActionsButton").performClick()
        compose.waitForIdle()
        Screengrab.screenshot("more_actions")

        compose.onNodeWithTag("createRuleFab").performClick()
        compose.waitForIdle()
        Screengrab.screenshot("create_rule")
    }
}