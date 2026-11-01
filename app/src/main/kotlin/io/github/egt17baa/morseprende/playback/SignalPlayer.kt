package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reproduce una línea de tiempo por varias salidas a la vez. Solo hay una reproducción activa:
 * empezar otra detiene la anterior, espera a que termine de cerrarse (varias salidas comparten
 * el mismo vibrador o la misma linterna) y solo entonces arranca la nueva.
 *
 * Si una salida falla, se avisa con `onOutputFailure` y las demás siguen sonando.
 */
class SignalPlayer(private val scope: CoroutineScope) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var job: Job? = null
    private var activeOutputs: List<SignalOutput> = emptyList()

    fun play(
        signals: List<Signal>,
        outputs: List<SignalOutput>,
        onOutputFailure: (SignalOutput) -> Unit = {},
    ) {
        val previous = job
        stop()
        if (signals.isEmpty() || outputs.isEmpty()) return

        activeOutputs = outputs
        _isPlaying.value = true
        val newJob = scope.launch(start = CoroutineStart.LAZY) {
            previous?.join()
            try {
                coroutineScope {
                    // La salida con retraso propio (el audio) marca el compás: las demás arrancan cuando
                    // avisa de que empieza a oírse. Antes esperaban un tiempo fijo y, como preparar el
                    // audio tarda más en unos móviles que en otros, el sonido iba por detrás.
                    val leader = outputs.filter { it.startLatencyMs > 0 }.maxByOrNull { it.startLatencyMs }
                    val started = CompletableDeferred<Unit>()
                    outputs.forEach { output ->
                        launch {
                            try {
                                if (output === leader) {
                                    output.play(signals) { started.complete(Unit) }
                                } else {
                                    if (leader != null) withTimeoutOrNull(leader.startLatencyMs + START_GRACE_MS) { started.await() }
                                    output.play(signals)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                output.stop()
                                onOutputFailure(output)
                            } finally {
                                // Si el audio falla o termina sin avisar, las demás no se quedan esperando.
                                if (output === leader) started.complete(Unit)
                            }
                        }
                    }
                }
            } finally {
                // Solo la reproducción vigente toca el estado: una anterior que termina tarde no
                // debe apagar las salidas de la nueva.
                if (job === coroutineContext[Job]) {
                    outputs.forEach { it.stop() }
                    job = null
                    activeOutputs = emptyList()
                    _isPlaying.value = false
                }
            }
        }
        job = newJob
        newJob.start()
    }

    companion object {
        /** Lo más que las demás salidas esperan, además del retraso previsto, a que el audio arranque. */
        const val START_GRACE_MS = 1_000L
    }

    fun stop() {
        job?.cancel()
        job = null
        activeOutputs.forEach { it.stop() }
        activeOutputs = emptyList()
        _isPlaying.value = false
    }
}
