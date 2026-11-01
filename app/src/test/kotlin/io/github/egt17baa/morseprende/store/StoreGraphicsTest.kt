package io.github.egt17baa.morseprende.store

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.data.AssetContentSource
import io.github.egt17baa.morseprende.game.content.Channel
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.translator.NoOpOutputFactory
import io.github.egt17baa.morseprende.translator.TranslatorScreen
import io.github.egt17baa.morseprende.translator.TranslatorViewModel
import io.github.egt17baa.morseprende.ui.games.GamePhase
import io.github.egt17baa.morseprende.ui.games.SignalView
import io.github.egt17baa.morseprende.ui.games.StormContent
import io.github.egt17baa.morseprende.ui.games.StormUiState
import io.github.egt17baa.morseprende.ui.hub.ChapterSection
import io.github.egt17baa.morseprende.ui.hub.HubCallbacks
import io.github.egt17baa.morseprende.ui.hub.HubContent
import io.github.egt17baa.morseprende.ui.hub.HubUiState
import io.github.egt17baa.morseprende.ui.hub.LighthouseScene
import io.github.egt17baa.morseprende.ui.hub.MissionNode
import io.github.egt17baa.morseprende.ui.library.LibraryContent
import io.github.egt17baa.morseprende.ui.library.LibraryUiState
import io.github.egt17baa.morseprende.ui.mission.MissionActions
import io.github.egt17baa.morseprende.ui.mission.MissionContent
import io.github.egt17baa.morseprende.ui.mission.MissionPhase
import io.github.egt17baa.morseprende.ui.mission.MissionUiState
import io.github.egt17baa.morseprende.ui.theme.Atkinson
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.MorsePrendeTheme
import io.github.egt17baa.morseprende.ui.theme.SeaNight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Gráficos de la ficha de Google Play. Solo se ejecuta con
 * `./gradlew :app:testDebugUnitTest --tests '*StoreGraphicsTest*' -PgraficosDir=$PWD/docs/publicacion/graficos`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-night-xxhdpi")
class StoreGraphicsTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val dir = System.getProperty("graficos.dir").orEmpty()
    private val content = AssetContentSource(ApplicationProvider.getApplicationContext<android.content.Context>())

    @Before fun setUp() {
        assumeTrue("sin -PgraficosDir no se generan los gráficos", dir.isNotEmpty())
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun save(name: String, width: Int, height: Int) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file = File(dir, name).apply { parentFile?.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Play rechaza los gráficos con otras medidas.
        val written = BitmapFactory.decodeFile(file.path)
        assertEquals("$name ancho", width, written.width)
        assertEquals("$name alto", height, written.height)
    }

    private object NoActions : MissionActions {
        override fun start() = Unit; override fun replay() = Unit; override fun continueListen() = Unit
        override fun answer(text: String) = Unit; override fun keyDown(atMs: Long) = Unit; override fun keyUp(atMs: Long) = Unit
        override fun tick() = Unit; override fun dot() = Unit; override fun dash() = Unit; override fun nextLetter() = Unit
        override fun deleteLast() = Unit; override fun submitSend() = Unit; override fun next() = Unit; override fun exit() = Unit
        override fun useSound() = Unit; override fun resume() = Unit
    }

    private fun screen(block: @androidx.compose.runtime.Composable () -> Unit) =
        compose.setContent { MorsePrendeTheme(darkTheme = true) { block() } }

    @Test @Config(qualifiers = "w256dp-h256dp-night-xhdpi")
    fun icon() {
        // Icono adaptativo (108 dp) recortado a su zona central: Play redondea las esquinas.
        compose.setContent {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val size = 256.dp * 108f / 84f
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(size))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size))
            }
        }
        save("icono-512.png", 512, 512)
    }

    @Test @Config(qualifiers = "w512dp-h250dp-night-xhdpi")
    fun feature() {
        compose.setContent {
            Row(Modifier.fillMaxSize().background(SeaNight), verticalAlignment = Alignment.CenterVertically) {
                LighthouseScene(0.8f, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1.1f).padding(horizontal = 24.dp)) {
                    Text("MorsePrende", color = BeamCream, fontSize = 34.sp, fontFamily = Atkinson, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                    Text("El Faro", color = BeamCream, fontSize = 30.sp, fontFamily = Atkinson)
                    Text("Aprende Morse jugando", color = androidx.compose.ui.graphics.Color(0xFFFF7A5C), fontSize = 20.sp, fontFamily = Atkinson, modifier = Modifier.padding(top = 12.dp))
                    Text("•  —  •  —", color = BeamCream.copy(alpha = 0.7f), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        save("destacado-1024x500.png", 1024, 500)
    }

    @Test fun shot1_hub() {
        val chapters = content.chapters()
        fun section(index: Int, done: Int): ChapterSection {
            val ch = chapters[index]
            val main = ch.missions
            val nodes = main.mapIndexed { i, m ->
                MissionNode(m.id, m.title, m.type, stars = if (i < done) (if (i % 3 == 1) 2 else 3) else 0, unlocked = i <= done, isNext = i == done, optional = m.optional)
            }
            return ChapterSection(ch.id, ch.title, nodes, unlocked = done >= 0, progress = done.toFloat() / main.size)
        }
        val state = HubUiState(
            chapterTitle = chapters[0].title,
            chapters = listOf(section(0, 4), section(1, -1), section(2, -1)),
            glow = 0.35f, balance = 140, streak = 4, newCards = 1,
        )
        screen { HubContent(state, HubCallbacks()) }
        save("captura-1-faro.png", 1080, 1920)
    }

    @Test fun shot2_receive() {
        val m = content.chapters()[1].missions.first { it.id == "c2m4" }
        screen {
            MissionContent(
                MissionUiState(
                    title = m.title, phase = MissionPhase.EXERCISE, exercise = Exercise.ReceiveChoice("F", listOf("H", "F", "L", "P")),
                    exerciseIndex = 5, exerciseCount = 14, noisy = true, secondsLeft = 12, chapterId = 2,
                ),
                NoActions,
            )
        }
        save("captura-2-recibir.png", 1080, 1920)
    }

    @Test fun shot3_send() {
        val m = content.chapters()[0].missions.first { it.id == "c1m1" }
        screen {
            MissionContent(
                MissionUiState(title = m.title, phase = MissionPhase.EXERCISE, exercise = Exercise.Send("SOS"), exerciseIndex = 7, exerciseCount = 12, sentText = "SO", keyerPending = ".."),
                NoActions,
            )
        }
        save("captura-3-enviar.png", 1080, 1920)
    }

    @Test fun shot4_light() {
        val m = content.chapters()[2].missions.first { it.id == "c3l" }
        screen {
            MissionContent(
                MissionUiState(
                    title = m.title, phase = MissionPhase.EXERCISE, exercise = Exercise.ReceiveTyped("ECO"),
                    exerciseIndex = 3, exerciseCount = 10, channel = Channel.LIGHT, lampOn = true, chapterId = 3,
                ),
                NoActions,
            )
        }
        save("captura-4-luz.png", 1080, 1920)
    }

    @Test fun shot5_library() {
        val card = content.libraryCards().first { it.id == "lib-sos" }
        screen { LibraryContent(LibraryUiState(opened = card), onOpen = {}, onClose = {}, onBack = {}) }
        save("captura-5-biblioteca.png", 1080, 1920)
    }

    @Test fun shot6_storm() {
        val letters = "ETANSOIMRKDUWG".toList()
        val signals = listOf(
            SignalView(1, 'K', "-.-", 0.18f), SignalView(2, 'S', "...", 0.42f),
            SignalView(3, 'A', ".-", 0.63f), SignalView(4, 'M', "--", 0.8f),
        )
        screen { StormContent(StormUiState(phase = GamePhase.PLAYING, letters = letters, signals = signals, score = 14, lives = 2), {}, {}, {}) }
        save("captura-6-tormenta.png", 1080, 1920)
    }

    @Test fun shot7_translator() {
        val vm = TranslatorViewModel(FakeSettingsRepository(), NoOpOutputFactory, torchAvailable = true)
        vm.onInputChange("Hola, faro")
        screen { TranslatorScreen(vm, onBack = {}) }
        save("captura-7-traductor.png", 1080, 1920)
    }
}
