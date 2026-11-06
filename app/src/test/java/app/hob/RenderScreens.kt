package app.hob

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.hob.data.DemoSeed
import app.hob.data.Settings
import app.hob.ui.HobRoot
import app.hob.ui.Overlay
import app.hob.ui.Tab
import app.hob.ui.onboarding.Onboarding
import app.hob.ui.theme.HobTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The main screens over the demo house, drawn to PNGs (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    open val theme = 0
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }
    private val app get() = ApplicationProvider.getApplicationContext<HobApp>()

    private fun seed(): Settings = runBlocking {
        val c = app.container
        c.seeding.first { !it }
        if (c.dao.placesNow().isEmpty()) DemoSeed.seedIfEmpty(c)
        c.seeding.value = false
        c.settings.update { it.copy(tipsOff = true) }
        c.settings.now()
    }

    private fun save(name: String) {
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun settle(waitFor: String?) {
        rule.waitUntil(30_000) { rule.mainClock.advanceTimeBy(100); waitFor == null || rule.onAllNodesWithText(waitFor, substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        Thread.sleep(700)
        rule.mainClock.advanceTimeBy(800)
    }

    @Test fun screens() {
        val st = seed()
        val c = app.container
        val (hall, box, jacket, vanya) = runBlocking {
            listOf(c.dao.roomsNow().first().id, c.dao.boxesNow().first().id, c.dao.thingsNow().first().id, c.dao.kidsNow().maxBy { it.born }.id)
        }
        val list = listOf(
            Triple("1-home", Tab.HOME, null),
            Triple("2-room", Tab.HOME, Overlay.Room(hall)),
            Triple("3-thing", Tab.HOME, Overlay.Thing(jacket)),
            Triple("4-find", Tab.FIND, null),
            Triple("5-people", Tab.PEOPLE, null),
            Triple("6-kid", Tab.PEOPLE, Overlay.Kid(vanya)),
            Triple("7-hob", Tab.HOB, null),
            Triple("8-clear", Tab.HOME, Overlay.ClearOut),
            Triple("9-box", Tab.HOME, Overlay.Box(box)),
            Triple("a-add", Tab.HOME, Overlay.Edit(room = hall)),
            Triple("b-settings", Tab.HOME, Overlay.Settings),
        )
        var which by mutableStateOf(list[0])
        rule.mainClock.autoAdvance = false
        rule.setContent { HobTheme(theme) { key(which.first) { HobRoot(st.copy(theme = theme), which.second, which.third) } } }
        for (sc in list) {
            rule.runOnUiThread { which = sc }
            settle(app.getString(R.string.tab_people))
            save(sc.first)
        }
    }

    @Test fun welcome() {
        rule.mainClock.autoAdvance = false
        rule.setContent { HobTheme(theme) { Onboarding() } }
        settle(null)
        save("0-welcome")
    }
}

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRuDay : ScreensBase() { override val lang = "ru-day"; override val theme = 1 }
