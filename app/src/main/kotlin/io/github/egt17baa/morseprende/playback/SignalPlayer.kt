package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Reproduce una línea de tiempo por varias salidas a la vez. Solo hay una reproducción activa:
 * empezar otra detiene la anterior y deja sus salidas apagadas.
 */
class SignalPlayer(private val scope: CoroutineScope) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var job: Job? = null
    private var activeOutputs: List<SignalOutput> = emptyList()

    fun play(signals: List<Signal>, outputs: List<SignalOutput>) {
        stop()
        if (signals.isEmpty() || outputs.isEmpty()) return

        activeOutputs = outputs
        _isPlaying.value = true
        val newJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                coroutineScope { outputs.forEach { output -> launch { output.play(signals) } } }
            } finally {
                outputs.forEach { it.stop() }
                if (job === coroutineContext[Job]) {
                    job = null
                    activeOutputs = emptyList()
                    _isPlaying.value = false
                }
            }
        }
        job = newJob
        newJob.start()
    }

    fun stop() {
        job?.cancel()
        job = null
        activeOutputs.forEach { it.stop() }
        activeOutputs = emptyList()
        _isPlaying.value = false
    }
}
