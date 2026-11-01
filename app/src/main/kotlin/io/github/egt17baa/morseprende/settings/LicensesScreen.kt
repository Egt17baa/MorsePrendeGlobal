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
import androidx.compose.ui.unit.dp
import io.github.egt17baa.morseprende.R

/** Ficheros de `assets/licenses/` en el orden en que se muestran, con su título. */
private val LICENSE_FILES = listOf(
    R.string.licenses_libraries to "licenses/bibliotecas.txt",
    R.string.licenses_font to "licenses/atkinson-hyperlegible-OFL.txt",
    R.string.licenses_apache to "licenses/apache-2.0.txt",
)

/** Licencias de código abierto de lo que va dentro de la app (Apache 2.0 y OFL las piden). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val texts = remember {
        LICENSE_FILES.map { (title, path) ->
            title to runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrDefault("")
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
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
            texts.forEach { (title, text) ->
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                // Párrafo a párrafo: un único Text enorme sería lento de medir.
                text.split("\n\n").filter { it.isNotBlank() }.forEach { Text(it.trim(), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
