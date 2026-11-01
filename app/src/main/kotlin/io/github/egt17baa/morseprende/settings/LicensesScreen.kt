package io.github.egt17baa.morseprende.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.egt17baa.morseprende.R

/** Ficheros de `assets/licenses/` en el orden en que se muestran, con su título. */
private val LICENSE_FILES: List<Pair<Int?, String>> = listOf(
    R.string.licenses_libraries to "licenses/bibliotecas.txt",
    R.string.licenses_font to "licenses/atkinson-hyperlegible-OFL.txt",
    R.string.licenses_apache to "licenses/apache-2.0.txt",
)

/** Licencias de código abierto de lo que va dentro de la app (Apache 2.0 y OFL las piden). */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    TextDocumentScreen(R.string.licenses_title, LICENSE_FILES, onBack)
}

/**
 * Política de privacidad dentro de la app (Play la pide también aquí, no solo en la web). El texto es
 * `docs/publicacion/politica-privacidad.md`, que Gradle copia a los assets al compilar.
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    TextDocumentScreen(R.string.privacy_title, listOf(null to "legal/politica-privacidad.md"), onBack)
}

/** Pantalla con uno o varios documentos de `assets/`, cada uno con un título opcional. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextDocumentScreen(title: Int, files: List<Pair<Int?, String>>, onBack: () -> Unit) {
    val context = LocalContext.current
    val texts = remember {
        files.map { (heading, path) ->
            heading to runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            texts.forEach { (heading, text) ->
                if (heading != null) {
                    Text(
                        stringResource(heading),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                if (text == null) Text(stringResource(R.string.document_missing)) else DocumentText(text)
            }
        }
    }
}
