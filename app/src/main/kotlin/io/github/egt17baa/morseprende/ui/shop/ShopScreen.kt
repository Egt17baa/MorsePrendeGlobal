package io.github.egt17baa.morseprende.ui.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.economy.BuyResult
import io.github.egt17baa.morseprende.game.economy.ItemCategory
import io.github.egt17baa.morseprende.ui.rewards.achievementTitle
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral
import io.github.egt17baa.morseprende.ui.theme.Cosmetics

@Composable
fun ShopScreen(viewModel: ShopViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShopContent(
        state,
        ShopActions(viewModel::buy, viewModel::equip, viewModel::toggleDecor, viewModel::preview, viewModel::dismissMessage),
        onBack,
    )
}

data class ShopActions(
    val buy: (String) -> Unit = {},
    val equip: (String) -> Unit = {},
    val toggleDecor: (String) -> Unit = {},
    val preview: (String) -> Unit = {},
    val dismiss: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopContent(state: ShopUiState, actions: ShopActions, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shop_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(pluralStringResource(R.plurals.shop_balance, state.balance, state.balance), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.shop_intro), style = MaterialTheme.typography.bodyMedium)
            Message(state, actions.dismiss)
            ItemCategory.entries.forEach { category ->
                Text(stringResource(category.titleRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                state.items.filter { it.item.category == category }.forEach { entry -> ItemRow(entry, actions) }
            }
        }
    }
}

@Composable
private fun Message(state: ShopUiState, dismiss: () -> Unit) {
    val result = state.lastResult
    val text = when {
        state.newAchievements.isNotEmpty() -> stringResource(R.string.reward_achievement, stringResource(achievementTitle(state.newAchievements.first())))
        result is BuyResult.NotEnough -> pluralStringResource(R.plurals.shop_not_enough, result.missing, result.missing)
        result == BuyResult.Bought -> stringResource(R.string.shop_bought)
        else -> return
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, modifier = Modifier.weight(1f))
            TextButton(onClick = dismiss) { Text(stringResource(R.string.shop_ok)) }
        }
    }
}

@Composable
private fun ItemRow(entry: ShopEntry, actions: ShopActions) {
    val id = entry.item.id
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemPreview(id, entry.item.category)
            Text(stringResource(itemName(id)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            if (entry.item.category == ItemCategory.TONE) {
                TextButton(onClick = { actions.preview(id) }) { Text(stringResource(R.string.shop_listen)) }
            }
            when {
                !entry.owned -> Button(onClick = { actions.buy(id) }, enabled = !entry.busy) {
                    Text(stringResource(R.string.shop_buy, entry.item.price))
                }
                entry.item.category == ItemCategory.DECOR -> OutlinedButton(onClick = { actions.toggleDecor(id) }) {
                    Text(stringResource(if (entry.equipped) R.string.shop_remove else R.string.shop_place))
                }
                entry.equipped -> Text(stringResource(R.string.shop_equipped), color = MaterialTheme.colorScheme.primary)
                else -> OutlinedButton(onClick = { actions.equip(id) }) { Text(stringResource(R.string.shop_equip)) }
            }
        }
    }
}

/** Vista previa dibujada de cada objeto. */
@Composable
private fun ItemPreview(id: String, category: ItemCategory) {
    Canvas(Modifier.size(40.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        when (category) {
            ItemCategory.LAMP -> {
                val color = Cosmetics.lampColor(id)
                drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = size.minDimension / 2), radius = size.minDimension / 2, center = c)
                drawCircle(color, radius = size.minDimension / 6, center = c)
            }
            ItemCategory.TONE -> {
                val w = size.width
                for (i in 0 until 5) {
                    val h = size.height * (0.2f + 0.15f * ((i * 7 + id.length) % 5))
                    drawLine(BeamCream, Offset(w * (0.15f + i * 0.17f), c.y - h / 2), Offset(w * (0.15f + i * 0.17f), c.y + h / 2), strokeWidth = w / 12)
                }
            }
            ItemCategory.KEYER -> drawCircle(Cosmetics.keyerRing(id), radius = size.minDimension / 2.6f, center = c, style = Stroke(width = size.minDimension / 8))
            ItemCategory.DECOR -> {
                drawCircle(Coral, radius = size.minDimension / 8, center = Offset(size.width * 0.3f, c.y))
                drawCircle(BeamCream, radius = size.minDimension / 8, center = Offset(size.width * 0.7f, c.y))
            }
        }
    }
}

private val ItemCategory.titleRes: Int
    get() = when (this) {
        ItemCategory.LAMP -> R.string.shop_cat_lamp
        ItemCategory.TONE -> R.string.shop_cat_tone
        ItemCategory.KEYER -> R.string.shop_cat_keyer
        ItemCategory.DECOR -> R.string.shop_cat_decor
    }

fun itemName(id: String): Int = when (id) {
    "lamp_cream" -> R.string.item_lamp_cream
    "lamp_emerald" -> R.string.item_lamp_emerald
    "lamp_harbor" -> R.string.item_lamp_harbor
    "lamp_polar" -> R.string.item_lamp_polar
    "lamp_amber" -> R.string.item_lamp_amber
    "tone_sine" -> R.string.item_tone_sine
    "tone_soft" -> R.string.item_tone_soft
    "tone_classic" -> R.string.item_tone_classic
    "key_coral" -> R.string.item_key_coral
    "key_brass" -> R.string.item_key_brass
    "key_silver" -> R.string.item_key_silver
    "decor_gulls" -> R.string.item_decor_gulls
    "decor_boat" -> R.string.item_decor_boat
    "decor_flags" -> R.string.item_decor_flags
    "lamp_violet" -> R.string.item_lamp_violet
    "key_wood" -> R.string.item_key_wood
    "decor_stars" -> R.string.item_decor_stars
    "decor_house" -> R.string.item_decor_house
    "decor_whale" -> R.string.item_decor_whale
    "decor_aurora" -> R.string.item_decor_aurora
    else -> R.string.item_decor_lanterns
}
