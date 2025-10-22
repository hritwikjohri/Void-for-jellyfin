package com.hritwik.avoid.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import java.util.concurrent.PriorityBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PriorityDispatcher @Inject constructor(
    private val okHttpDispatcher: Dispatcher
) {
    enum class Priority { HIGH, LOW }

    private data class Task(
        val priority: Priority,
        val block: suspend () -> Unit
    ) : Comparable<Task> {
        override fun compareTo(other: Task): Int = priority.ordinal - other.priority.ordinal
    }

    private val queue = PriorityBlockingQueue<Task>()

    init {
        okHttpDispatcher.executorService.execute {
            while (true) {
                val task = queue.take()
                runBlocking { task.block() }
            }
        }
    }

    fun enqueue(priority: Priority, block: suspend () -> Unit) {
        queue.offer(Task(priority, block))
    }
}

