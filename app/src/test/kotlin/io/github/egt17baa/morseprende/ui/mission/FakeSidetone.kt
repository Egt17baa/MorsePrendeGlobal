package io.github.egt17baa.morseprende.ui.mission

import io.github.egt17baa.morseprende.playback.Sidetone
import io.github.egt17baa.morseprende.playback.SidetoneFactory

class FakeSidetone : Sidetone {
    val calls = mutableListOf<String>()
    val closed: Boolean get() = "close" in calls
    override fun keyDown() { calls += "down" }
    override fun keyUp() { calls += "up" }
    override fun close() { calls += "close" }
    var warmed = false
    override fun warmUp() { warmed = true }
}

class FakeSidetoneFactory : SidetoneFactory {
    val created = mutableListOf<FakeSidetone>()
    override fun create(settings: io.github.egt17baa.morseprende.settings.AppSettings) = FakeSidetone().also { created += it }
    val last: FakeSidetone get() = created.last()
}
