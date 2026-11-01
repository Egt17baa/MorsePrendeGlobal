package io.github.egt17baa.morseprende.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class AssetContentSourceTest {

    @Test fun asset_content_loads_both_chapters() {
        val source = AssetContentSource(ApplicationProvider.getApplicationContext<android.content.Context>())
        val chapters = source.chapters()
        assertEquals(listOf(1, 2), chapters.map { it.id })
        assertEquals(8, chapters[0].missions.size)
        assertEquals(19, chapters[1].missions.size)
    }

    @Test fun broken_json_throws_content_load_exception() {
        val source = AssetContentSource { "{" }
        assertFailsWith<ContentLoadException> { source.chapters() }
    }

    @Test fun missing_file_throws_content_load_exception() {
        val source = AssetContentSource { throw java.io.FileNotFoundException(it) }
        assertFailsWith<ContentLoadException> { source.libraryCards() }
    }
}
