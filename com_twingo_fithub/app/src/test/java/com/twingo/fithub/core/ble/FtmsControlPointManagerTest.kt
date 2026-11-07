package com.twingo.fithub.core.ble

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FtmsControlPointManagerTest {

    @Test
    fun initialStateIsIdle() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        val manager = FtmsControlPointManager(scope, queue) { _: ByteArray -> true }
        assertEquals(FtmsControlPointManager.ControlState.IDLE, manager.state.first())
        queue.close()
        scope.cancel()
    }

    @Test
    fun transitionsToAcquiredOnSuccessSequence() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        var sentCommands = mutableListOf<ByteArray>()

        val manager = FtmsControlPointManager(scope, queue) { cmd: ByteArray ->
            sentCommands += cmd
            true
        }

        // Lancer l'acquisition
        val acquireJob = scope.async { manager.acquireControl() }

        // Simuler la réponse à Request Control
        delay(600) // attendre SUBSCRIBING + REQUESTING
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x01.toByte()))

        // Simuler la réponse à Start/Resume
        delay(100)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x07.toByte(), 0x01.toByte()))

        val result = acquireJob.await()
        assertTrue(result)
        assertEquals(FtmsControlPointManager.ControlState.CONTROL_ACQUIRED, manager.state.first())

        // Vérifier les commandes envoyées
        assertEquals(2, sentCommands.size) // Request Control + Start/Resume
        assertArrayEquals(byteArrayOf(0x00.toByte()), sentCommands[0])
        assertArrayEquals(byteArrayOf(0x07.toByte()), sentCommands[1])

        queue.close()
        scope.cancel()
    }

    @Test
    fun failsIfRequestControlRejected() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)

        val manager = FtmsControlPointManager(scope, queue) { _: ByteArray -> true }

        val acquireJob = scope.async { manager.acquireControl() }

        delay(600)
        // Réponse : Control Not Permitted
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x05.toByte()))

        val result = acquireJob.await()
        assertFalse(result)
        assertEquals(FtmsControlPointManager.ControlState.FAILED, manager.state.first())

        queue.close()
        scope.cancel()
    }

    @Test
    fun failsIfStartResumeRejected() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)

        val manager = FtmsControlPointManager(scope, queue) { _: ByteArray -> true }

        val acquireJob = scope.async { manager.acquireControl() }

        delay(600)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x01.toByte())) // Request Control OK

        delay(100)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x07.toByte(), 0x02.toByte())) // Start/Resume Not Supported

        val result = acquireJob.await()
        assertFalse(result)
        assertEquals(FtmsControlPointManager.ControlState.FAILED, manager.state.first())

        queue.close()
        scope.cancel()
    }

    @Test
    fun sendsSetTargetPowerWhenAcquired() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        var sentCommands = mutableListOf<ByteArray>()

        val manager = FtmsControlPointManager(scope, queue) { cmd: ByteArray ->
            sentCommands += cmd
            true
        }

        // Acquérir le contrôle
        val acquireJob = scope.async { manager.acquireControl() }
        delay(600)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x01.toByte()))
        delay(100)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x07.toByte(), 0x01.toByte()))
        acquireJob.join()

        // Envoyer une cible de puissance
        scope.async { manager.setTargetPower(200) }.await()

        // Vérifier : Request Control + Start/Resume + Set Target Power
        assertEquals(3, sentCommands.size)
        assertArrayEquals(byteArrayOf(0x05.toByte(), (-56).toByte(), 0x00.toByte()), sentCommands[2]) // 200 = 0x00C8

        queue.close()
        scope.cancel()
    }

    @Test
    fun rejectsSetTargetPowerWhenNotAcquired() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        var sentCommands = mutableListOf<ByteArray>()

        val manager = FtmsControlPointManager(scope, queue) { cmd: ByteArray ->
            sentCommands += cmd
            true
        }

        // Ne pas acquérir le contrôle, essayer d'envoyer directement
        val result = scope.async { manager.setTargetPower(200) }.await()

        assertFalse(result)
        assertTrue(sentCommands.isEmpty())

        queue.close()
        scope.cancel()
    }

    @Test
    fun sendsPauseAndResume() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        var sentCommands = mutableListOf<ByteArray>()

        val manager = FtmsControlPointManager(scope, queue) { cmd: ByteArray ->
            sentCommands += cmd
            true
        }

        // Acquérir le contrôle
        val acquireJob = scope.async { manager.acquireControl() }
        delay(600)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x01.toByte()))
        delay(100)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x07.toByte(), 0x01.toByte()))
        acquireJob.join()

        // Pause
        scope.async { manager.pause() }.await()
        // Resume
        scope.async { manager.resume() }.await()

        assertEquals(4, sentCommands.size) // Request + Start + Pause + Resume
        assertArrayEquals(byteArrayOf(0x08.toByte()), sentCommands[2]) // Pause
        assertArrayEquals(byteArrayOf(0x07.toByte()), sentCommands[3]) // Resume

        queue.close()
        scope.cancel()
    }

    @Test
    fun releaseControlSendsReset() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        var sentCommands = mutableListOf<ByteArray>()

        val manager = FtmsControlPointManager(scope, queue) { cmd: ByteArray ->
            sentCommands += cmd
            true
        }

        // Acquérir le contrôle
        val acquireJob = scope.async { manager.acquireControl() }
        delay(600)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x01.toByte()))
        delay(100)
        manager.handleIndication(byteArrayOf(0x80.toByte(), 0x07.toByte(), 0x01.toByte()))
        acquireJob.join()

        // Libérer
        manager.releaseControl()
        delay(100)

        // Reset doit avoir été envoyé
        assertEquals(3, sentCommands.size) // Request + Start + Reset
        assertArrayEquals(byteArrayOf(0x01.toByte()), sentCommands[2])
        assertEquals(FtmsControlPointManager.ControlState.IDLE, manager.state.first())

        queue.close()
        scope.cancel()
    }
}