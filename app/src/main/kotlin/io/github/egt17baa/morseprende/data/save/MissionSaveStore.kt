package io.github.egt17baa.morseprende.data.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Partidas a medias (una por misión), guardadas como texto JSON. */
interface MissionSaveStore {
    suspend fun load(missionId: String): String?
    suspend fun save(missionId: String, json: String)
    suspend fun clear(missionId: String)
}

/** Sin guardado (pruebas que no lo necesitan). */
object NoMissionSaves : MissionSaveStore {
    override suspend fun load(missionId: String): String? = null
    override suspend fun save(missionId: String, json: String) = Unit
    override suspend fun clear(missionId: String) = Unit
}

/**
 * Un fichero por misión en la carpeta privada de la app. Se escribe en un temporal y se renombra,
 * así un cierre a mitad nunca deja un fichero roto.
 */
class FileMissionSaveStore(private val dir: File) : MissionSaveStore {

    private fun fileFor(missionId: String): File {
        // Los ids son del contenido propio (c1m2…), pero por si acaso solo se admiten caracteres seguros.
        val safe = missionId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        return File(dir, "$safe.json")
    }

    override suspend fun load(missionId: String): String? = withContext(Dispatchers.IO) {
        runCatching { fileFor(missionId).takeIf { it.exists() }?.readText() }.getOrNull()
    }

    override suspend fun save(missionId: String, json: String) = withContext(Dispatchers.IO) {
        runCatching {
            dir.mkdirs()
            val target = fileFor(missionId)
            val tmp = File(dir, target.name + ".tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(target)) {
                target.delete()
                tmp.renameTo(target)
            }
        }
        Unit
    }

    override suspend fun clear(missionId: String) = withContext(Dispatchers.IO) {
        runCatching { fileFor(missionId).delete() }
        Unit
    }
}
