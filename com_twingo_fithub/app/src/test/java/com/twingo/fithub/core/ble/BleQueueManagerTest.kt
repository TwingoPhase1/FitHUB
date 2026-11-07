package com.twingo.fithub.core.ble

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BleQueueManagerTest {

    @Test
    fun executesOperationsOneAtATimeInFifoOrder() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        val events = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()

        try {
            val first = async(Dispatchers.Default) {
                queue.execute(2_000) {
                    events += "start-1"
                    gate.await()
                    events += "end-1"
                    1
                }
            }
            withTimeout(1_000) {
                while (events.isEmpty()) delay(10)
            }

            val second = async(Dispatchers.Default) {
                queue.execute(2_000) {
                    events += "start-2"
                    2
                }
            }

            delay(100)
            assertEquals(listOf("start-1"), events)

            gate.complete(Unit)
            assertEquals(1, first.await())
            assertEquals(2, second.await())
            assertEquals(listOf("start-1", "end-1", "start-2"), events)
        } finally {
            queue.close()
            scope.cancel()
        }
    }

    @Test
    fun timeoutReleasesTheQueueForTheNextOperation() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = BleQueueManager(scope)
        val never = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()

        try {
            val first = async(Dispatchers.Default) {
                runCatching {
                    queue.execute(150) {
                        events += "slow"
                        never.await()
                    }
                }
            }

            val second = async(Dispatchers.Default) {
                queue.execute(1_000) {
                    events += "next"
                    7
                }
            }

            first.await()
            assertEquals(7, second.await())
            assertEquals(listOf("slow", "next"), events)
            assertTrue(events.indexOf("next") > events.indexOf("slow"))
        } finally {
            queue.close()
            scope.cancel()
        }
    }
}
