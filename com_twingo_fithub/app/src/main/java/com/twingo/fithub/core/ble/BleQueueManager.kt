package com.twingo.fithub.core.ble

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * File FIFO stricte pour les opérations GATT d'une connexion.
 *
 * Une action n'est dépilée qu'après la fin de la précédente. Chaque action
 * suspend jusqu'au callback Android attendu ; un timeout libère la file afin
 * d'éviter un blocage définitif sur un périphérique défaillant.
 *
 * Cette classe est volontairement sans dépendance Android pour être testable
 * sur la JVM.
 */
class BleQueueManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val pending = mutableSetOf<CompletableDeferred<*>>()
    private val pendingLock = Any()

    private val processor = scope.launch {
        for (action in queue) {
            action()
        }
    }

    suspend fun <T> execute(timeoutMillis: Long, action: suspend () -> T): T {
        val result = CompletableDeferred<T>()
        synchronized(pendingLock) { pending += result }
        try {
            queue.send {
                try {
                    result.complete(withTimeout(timeoutMillis) { action() })
                } catch (timeout: TimeoutCancellationException) {
                    result.completeExceptionally(timeout)
                } catch (cancellation: CancellationException) {
                    result.completeExceptionally(cancellation)
                    throw cancellation
                } catch (error: Throwable) {
                    result.completeExceptionally(error)
                }
            }
            return result.await()
        } finally {
            synchronized(pendingLock) { pending -= result }
        }
    }

    fun close() {
        queue.close()
        synchronized(pendingLock) {
            pending.forEach { it.cancel() }
            pending.clear()
        }
        processor.cancel()
    }
}
