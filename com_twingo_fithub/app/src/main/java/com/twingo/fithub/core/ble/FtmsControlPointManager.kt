package com.twingo.fithub.core.ble

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Machine à états pour la prise de contrôle FTMS (Control Point 0x2AD9).
 *
 * Séquence obligatoire :
 * 1. SUBSCRIBING_INDICATIONS  - écriture CCCD 0x2902 = 0x02 0x00
 * 2. REQUESTING_CONTROL       - envoi 0x00, attente réponse 0x80 0x00 0x01
 * 3. STARTING                 - envoi 0x07
 * 4. CONTROL_ACQUIRED         - contrôle actif, on peut envoyer 0x05/0x04/0x03
 * 5. RELEASING                - envoi 0x01 (Reset) à la fin
 */
class FtmsControlPointManager(
    private val scope: CoroutineScope,
    private val queue: BleQueueManager,
    private val writeControlPoint: suspend (ByteArray) -> Boolean,
) {

    enum class ControlState {
        IDLE,
        SUBSCRIBING_INDICATIONS,
        REQUESTING_CONTROL,
        STARTING,
        CONTROL_ACQUIRED,
        RELEASING,
        FAILED,
    }

    private val _state = MutableStateFlow(ControlState.IDLE)
    val state: StateFlow<ControlState> = _state.asStateFlow()

    private var responseDeferred: CompletableDeferred<FtmsControlPoint.ControlPointResponse>? = null
    private var acquireJob: Job? = null

    fun handleIndication(data: ByteArray) {
        val response = FtmsControlPoint.parseResponse(data)
        response?.let {
            responseDeferred?.complete(it)
            responseDeferred = null
        }
    }

    /**
     * Lance la séquence complète de prise de contrôle.
     * Retourne true si le contrôle est acquis, false sinon.
     */
    suspend fun acquireControl(): Boolean {
        if (_state.value != ControlState.IDLE && _state.value != ControlState.FAILED) {
            return _state.value == ControlState.CONTROL_ACQUIRED
        }
        acquireJob?.cancel()
        val job = scope.launch {
            awaitAcquire()
        }
        acquireJob = job
        return try {
            job.join()
            _state.value == ControlState.CONTROL_ACQUIRED
        } catch (cancellation: CancellationException) {
            false
        }
    }

    private suspend fun awaitAcquire() {
        try {
            // 1. S'abonner aux indications (CCCD = 0x02 0x00)
            _state.value = ControlState.SUBSCRIBING_INDICATIONS
            // NOTE: l'écriture du CCCD est gérée par BleDeviceConnection.enableNotifications()
            // on attend juste que le descripteur soit écrit
            delay(500) // laisse le temps au CCCD de s'écrire

            // 2. Request Control (0x00)
            _state.value = ControlState.REQUESTING_CONTROL
            val reqResponse = sendCommandWithResponse(
                FtmsControlPoint.encodeRequestControl(),
                FtmsControlPoint.OpCode.REQUEST_CONTROL,
                timeoutMs = 5000,
            )
            if (reqResponse?.result != FtmsControlPoint.ResponseCode.SUCCESS) {
                _state.value = ControlState.FAILED
                return
            }

            // 3. Start or Resume (0x07)
            _state.value = ControlState.STARTING
            val startResponse = sendCommandWithResponse(
                FtmsControlPoint.encodeStartOrResume(),
                FtmsControlPoint.OpCode.START_OR_RESUME,
                timeoutMs = 5000,
            )
            if (startResponse?.result != FtmsControlPoint.ResponseCode.SUCCESS) {
                _state.value = ControlState.FAILED
                return
            }

            // 4. Contrôle acquis
            _state.value = ControlState.CONTROL_ACQUIRED

        } catch (_: CancellationException) {
            _state.value = ControlState.FAILED
        }
    }

    private suspend fun sendCommandWithResponse(
        command: ByteArray,
        expectedOpCode: FtmsControlPoint.OpCode,
        timeoutMs: Long,
    ): FtmsControlPoint.ControlPointResponse? {
        val deferred = CompletableDeferred<FtmsControlPoint.ControlPointResponse>()
        responseDeferred = deferred

        val sent = queue.execute(timeoutMs) {
            writeControlPoint(command)
        }
        if (!sent) {
            responseDeferred = null
            return null
        }

        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) { deferred.await() }
        } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
            responseDeferred = null
            null
        } catch (cancellation: CancellationException) {
            responseDeferred = null
            throw cancellation
        } catch (_: Exception) {
            responseDeferred = null
            null
        }
    }

    /** Envoie Set Target Power (mode ERG) si contrôle acquis. */
    suspend fun setTargetPower(watts: Int): Boolean {
        if (_state.value != ControlState.CONTROL_ACQUIRED) return false
        val cmd = FtmsControlPoint.encodeSetTargetPower(watts)
        return queue.execute(3000) { writeControlPoint(cmd) }
    }

    /** Envoie Set Target Resistance si contrôle acquis. */
    suspend fun setTargetResistance(level: Int): Boolean {
        if (_state.value != ControlState.CONTROL_ACQUIRED) return false
        val cmd = FtmsControlPoint.encodeSetTargetResistance(level)
        return queue.execute(3000) { writeControlPoint(cmd) }
    }

    /** Envoie Set Target Incline si contrôle acquis. */
    suspend fun setTargetIncline(percent: Float): Boolean {
        if (_state.value != ControlState.CONTROL_ACQUIRED) return false
        val cmd = FtmsControlPoint.encodeSetTargetIncline(percent)
        return queue.execute(3000) { writeControlPoint(cmd) }
    }

    /** Envoie Pause (0x08) si contrôle acquis. */
    suspend fun pause(): Boolean {
        if (_state.value != ControlState.CONTROL_ACQUIRED) return false
        val cmd = FtmsControlPoint.encodePause()
        return queue.execute(3000) { writeControlPoint(cmd) }
    }

    /** Envoie Resume (0x07) si contrôle acquis. */
    suspend fun resume(): Boolean {
        if (_state.value != ControlState.CONTROL_ACQUIRED) return false
        val cmd = FtmsControlPoint.encodeStartOrResume()
        return queue.execute(3000) { writeControlPoint(cmd) }
    }

    /**
     * Libère le contrôle (Reset 0x01) et remet l'état à IDLE.
     * À appeler à la fin de séance ou à la déconnexion.
     */
    fun releaseControl() {
        acquireJob?.cancel()
        acquireJob = null
        if (_state.value == ControlState.CONTROL_ACQUIRED) {
            _state.value = ControlState.RELEASING
            scope.launch {
                queue.execute(3000) {
                    writeControlPoint(FtmsControlPoint.encodeReset())
                }
                _state.value = ControlState.IDLE
            }
        } else {
            _state.value = ControlState.IDLE
        }
    }
}