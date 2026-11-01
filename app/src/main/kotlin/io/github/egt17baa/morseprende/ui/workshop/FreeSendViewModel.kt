package io.github.egt17baa.morseprende.ui.workshop

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.KeyerDecoder
import io.github.egt17baa.morseprende.core.KeyerEvent
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.playback.Sidetone
import io.github.egt17baa.morseprende.playback.SidetoneFactory
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.KeyerMode
import io.github.egt17baa.morseprende.settings.SettingsRepository
import io.github.egt17baa.morseprende.ui.mission.KeyerActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FreeSendUiState(
    /** Lo descifrado hasta ahora. */
    val text: String = "",
    /** Puntos y rayas de la letra que se está tecleando. */
    val pending: String = "",
    /** Velocidad a la que teclea el jugador (se adapta a su ritmo). */
    val wpm: Int = 0,
    /** Ya se ha tecleado algo con el manipulador (hasta entonces la velocidad no dice nada). */
    val keyed: Boolean = false,
    val keyerMode: KeyerMode = KeyerMode.ONE_BUTTON,
    val keyerSkin: String = "key_coral",
    val haptics: Boolean = true,
)

/**
 * «Envío libre» del Taller: se teclea lo que se quiera y se ve qué letras salen y a qué velocidad.
 * No puntúa ni guarda nada.
 */
class FreeSendViewModel(
    private val settingsRepo: SettingsRepository,
    private val sidetones: SidetoneFactory,
    private val clock: () -> Long = { SystemClock.uptimeMillis() },
) : ViewModel(), KeyerActions {

    private val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val ui = MutableStateFlow(FreeSendUiState())
    private var keyer = KeyerDecoder(1200L / AppSettings().effectiveWpm)
    private var sidetone: Sidetone? = null
    private var keyIsDown = false

    val state: StateFlow<FreeSendUiState> = combine(ui, this.settings) { s, prefs ->
        s.copy(keyerMode = prefs.keyerMode, keyerSkin = prefs.keyerSkin, haptics = prefs.vibration)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FreeSendUiState())

    /** Cada entrada y salida; una entrada que termina de cargar después de salir no abre nada. */
    private var visit = 0

    /**
     * Al entrar en la pantalla: el tono se prepara ya para que el primer punto no se pierda. Se
     * esperan los ajustes de verdad (DataStore tarda un poco): si no, el tono saldría con el tono,
     * el volumen y la velocidad de fábrica.
     */
    fun enter() {
        val mine = ++visit
        viewModelScope.launch {
            val prefs = settingsRepo.settings.first()
            if (mine != visit) return@launch
            keyer = KeyerDecoder(1200L / prefs.effectiveWpm)
            ui.update { it.copy(pending = "", wpm = wpmOf(keyer)) }
            (sidetone ?: sidetones.create(prefs).also { sidetone = it }).warmUp()
        }
    }

    /** Al salir o pasar a segundo plano: el tono nunca se queda sonando. */
    fun leave() {
        visit++
        releaseKey()
        sidetone?.close()
        sidetone = null
        keyer.reset()
        ui.update { it.copy(pending = "") }
    }

    override fun keyDown(atMs: Long) {
        keyer.press(atMs)
        val tone = sidetone ?: sidetones.create(settings.value).also { sidetone = it }
        keyIsDown = true
        tone.keyDown()
    }

    override fun keyUp(atMs: Long) {
        releaseKey()
        keyer.release(atMs)
        ui.update { it.copy(pending = keyer.pending, wpm = wpmOf(keyer), keyed = true) }
    }

    /** La pantalla lo llama cada ~20 ms: cierra letras y palabras por las pausas. */
    fun tick() {
        when (val event = keyer.poll(clock())) {
            is KeyerEvent.LetterEnded -> append(event.char ?: MorseCodec.UNKNOWN)
            KeyerEvent.WordEnded -> appendSpace()
            null -> return
        }
        ui.update { it.copy(pending = keyer.pending) }
    }

    override fun dot() = element('.')

    override fun dash() = element('-')

    override fun nextLetter() {
        val event = keyer.commitLetter()
        if (event != null) append(event.char ?: MorseCodec.UNKNOWN) else appendSpace()
        ui.update { it.copy(pending = keyer.pending) }
    }

    fun deleteLast() {
        // reset() también olvida la pausa en curso: borrar en mitad de una pausa no deja un espacio suelto.
        if (keyer.pending.isEmpty()) ui.update { it.copy(text = it.text.dropLast(1)) }
        keyer.reset()
        ui.update { it.copy(pending = keyer.pending) }
    }

    fun clear() {
        keyer.reset()
        ui.update { it.copy(text = "", pending = "") }
    }

    private fun element(e: Char) {
        keyer.appendElement(e)
        ui.update { it.copy(pending = keyer.pending) }
    }

    private fun append(c: Char) {
        ui.update { it.copy(text = (it.text + c).takeLast(MAX_CHARS)) }
    }

    private fun appendSpace() {
        ui.update { if (it.text.isEmpty() || it.text.endsWith(' ')) it else it.copy(text = (it.text + ' ').takeLast(MAX_CHARS)) }
    }

    private fun releaseKey() {
        if (keyIsDown) {
            keyIsDown = false
            sidetone?.keyUp()
        }
    }

    private fun wpmOf(k: KeyerDecoder): Int = (1200L / k.unitMs.coerceAtLeast(1)).toInt()

    override fun onCleared() = leave()

    companion object {
        const val MAX_CHARS = 200
    }
}
