package io.github.egt17baa.morseprende.ui.hub

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import io.github.egt17baa.morseprende.game.economy.ItemCategory
import io.github.egt17baa.morseprende.game.economy.Shop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LighthouseSceneTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private var decorations by androidx.compose.runtime.mutableStateOf(emptySet<String>())

    private fun render(set: Set<String>): Bitmap {
        decorations = set
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }

    @Test fun scene_draws_all_decorations() {
        val all = Shop.items.filter { it.category == ItemCategory.DECOR }.map { it.id }.toSet()
        assertEquals(8, all.size)
        // Se dibuja entera sin fallar con todas las decoraciones a la vez, y se nota la diferencia.
        compose.setContent { LighthouseScene(1f, Modifier.fillMaxWidth().height(220.dp), decorations = decorations) }
        val plain = render(emptySet())
        val decorated = render(all)
        assertTrue(!plain.sameAs(decorated))
    }

    private var previewId by androidx.compose.runtime.mutableStateOf("")

    @Test fun each_decoration_has_its_own_preview() {
        val ids = Shop.items.filter { it.category == ItemCategory.DECOR }.map { it.id }
        // En la tienda se ve la isla con esa decoración: así se sabe qué se compra.
        compose.setContent { DecorPreview(previewId, Modifier.fillMaxWidth().height(96.dp)) }
        val shots = ids.map { id ->
            previewId = id
            compose.waitForIdle()
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        for (a in shots.indices) for (b in a + 1 until shots.size) {
            assertTrue("${ids[a]} y ${ids[b]} se ven iguales", !shots[a].sameAs(shots[b]))
        }
    }
}
