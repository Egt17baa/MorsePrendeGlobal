package io.github.egt17baa.morseprende.data.save

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MissionSaveStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun save_load_clear() = runTest {
        val store = FileMissionSaveStore(tmp.newFolder("saves"))
        assertNull(store.load("c1m2"))
        store.save("c1m2", "{\"a\":1}")
        store.save("c1m2", "{\"a\":2}")
        assertEquals("{\"a\":2}", store.load("c1m2"))
        assertNull(store.load("c1m3"))
        store.clear("c1m2")
        assertNull(store.load("c1m2"))
        store.save("a", "1"); store.save("b", "2")
        store.clearAll()
        assertNull(store.load("a")); assertNull(store.load("b"))
    }

    @Test fun survives_a_new_instance() = runTest {
        val dir = tmp.newFolder("saves")
        FileMissionSaveStore(dir).save("c2m1", "x")
        assertEquals("x", FileMissionSaveStore(dir).load("c2m1"))
    }
}

/** Almacén en memoria para las pruebas del ViewModel. */
class InMemoryMissionSaveStore : MissionSaveStore {
    val saved = mutableMapOf<String, String>()
    override suspend fun load(missionId: String): String? = saved[missionId]
    override suspend fun save(missionId: String, json: String) { saved[missionId] = json }
    override suspend fun clear(missionId: String) { saved.remove(missionId) }
    override suspend fun clearAll() { saved.clear() }
}
