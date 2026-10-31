package io.github.egt17baa.morseprende.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class AssetContentSourceTest {

    @Test fun asset_content_loads_chapter1() {
        val source = AssetContentSource(ApplicationProvider.getApplicationContext<android.content.Context>())
        assertEquals(8, source.chapters().single().missions.size)
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
