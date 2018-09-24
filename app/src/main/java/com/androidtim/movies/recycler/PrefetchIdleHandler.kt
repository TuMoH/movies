package com.androidtim.movies.recycler

import android.os.Looper
import android.os.MessageQueue
import android.view.LayoutInflater
import java.util.*

class PrefetchIdleHandler(private val count: Int,
                          private val inflater: LayoutInflater,
                          private var onItemDismissListener: OnItemDismissListener,
                          private var onItemItemMenuClickListener: OnItemMenuClickListener
) : MessageQueue.IdleHandler {

    private var isFinished = false
    private var createdCount = 0
    private val prefetchPool = ArrayDeque<ItemViewHolder>()

    override fun queueIdle(): Boolean {
        if (isFinished) return false

        prefetchPool.add(ItemViewHolder.create(inflater, null, onItemDismissListener, onItemItemMenuClickListener))
        createdCount++
        isFinished = createdCount >= count

        return !isFinished
    }

    fun run() {
        if (isFinished) return

        Looper.myQueue().addIdleHandler(this)
    }

    fun stop() {
        if (isFinished) return

        isFinished = true
        Looper.myQueue().removeIdleHandler(this)
    }

    fun getItemViewHolder(): ItemViewHolder? {
        return prefetchPool.poll()
    }

}