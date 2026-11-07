package com.example.vivophone.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow

object CallManager {
    val call = MutableStateFlow<Call?>(null)
    val state = MutableStateFlow(Call.STATE_NEW)
    val muted = MutableStateFlow(false)
    val speaker = MutableStateFlow(false)
    var service: InCallService? = null

    private val cb = object : Call.Callback() {
        override fun onStateChanged(c: Call, s: Int) { state.value = s }
    }

    fun add(c: Call) {
        call.value = c
        state.value = c.state
        c.registerCallback(cb)
    }

    fun remove(c: Call) {
        c.unregisterCallback(cb)
        if (call.value == c) {
            call.value = null
            state.value = Call.STATE_DISCONNECTED
        }
    }

    fun answer() = call.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
    fun reject() = call.value?.reject(false, null)
    fun hangup() = call.value?.disconnect()
    fun hold(on: Boolean) { if (on) call.value?.hold() else call.value?.unhold() }

    fun mute(on: Boolean) {
        service?.setMuted(on)
        muted.value = on
    }

    fun speaker(on: Boolean) {
        service?.setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        speaker.value = on
    }

    fun dtmf(ch: Char) {
        call.value?.playDtmfTone(ch)
        call.value?.stopDtmfTone()
    }

    val number: String
        get() = call.value?.details?.handle?.schemeSpecificPart ?: ""
}
