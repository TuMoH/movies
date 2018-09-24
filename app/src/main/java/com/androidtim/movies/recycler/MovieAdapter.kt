package com.androidtim.movies.recycler

import android.os.Looper
import android.os.MessageQueue
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.androidtim.movies.recycler.model.GenreListItem
import com.androidtim.movies.recycler.model.MovieListItem
import java.util.*

typealias OnItemDismissListener = (adapterPosition: Int, genre: GenreListItem, movie: MovieListItem) -> Unit
typealias OnItemMenuClickListener = (adapterPosition: Int, genre: GenreListItem, movie: MovieListItem) -> Unit
typealias OnHeaderClickListener = (adapterPosition: Int, genre: GenreListItem) -> Unit

class MovieAdapter(private val data: List<GenreListItem>,
                   private val inflater: LayoutInflater) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val HEADER_VIEW_TYPE = 1
        private const val ITEM_VIEW_TYPE = 2
    }

    var onItemDismissListener: OnItemDismissListener? = null
    var onItemItemMenuClickListener: OnItemMenuClickListener? = null
    var onHeaderClickListener: OnHeaderClickListener? = null

    private var flatData = ArrayList<IndexHolder>()
    private val prefetchIdleHandler = PrefetchIdleHandler(5)

    init {
        updateFlatData()
        registerAdapterDataObserver(DataObserver())
        prefetchIdleHandler.run()
    }

    fun updateFlatData() {
        flatData.clear()

        for (genreIndex in data.indices) {
            val genre = data[genreIndex]
            flatData.add(IndexHolder(HEADER_VIEW_TYPE, genreIndex))

            if (genre.expanded) {
                for (movieIndex in genre.movies.indices) {
                    flatData.add(IndexHolder(ITEM_VIEW_TYPE, genreIndex, movieIndex))
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return flatData.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (flatData[position].type == HEADER_VIEW_TYPE) {
            HEADER_VIEW_TYPE
        } else {
            ITEM_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == HEADER_VIEW_TYPE) {
            return HeaderViewHolder.create(inflater, parent, onHeaderClickListener)
        } else {
            val prefetched = prefetchIdleHandler.getItemViewHolder()
            if (prefetched != null) {
                return prefetched
            }
            prefetchIdleHandler.stop()

            return ItemViewHolder.create(inflater, parent, onItemDismissListener, onItemItemMenuClickListener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val indexHolder = flatData[position]
        val genreListItem = data[indexHolder.genreIndex]

        if (holder is HeaderViewHolder) {
            holder.bind(genreListItem)
        } else if (holder is ItemViewHolder) {
            holder.bind(genreListItem, genreListItem.movies[indexHolder.movieIndex])
        }
    }


    inner class DataObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateFlatData()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateFlatData()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            updateFlatData()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateFlatData()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            updateFlatData()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            updateFlatData()
        }
    }

    class IndexHolder(val type: Int, val genreIndex: Int, val movieIndex: Int = -1)

    inner class PrefetchIdleHandler(private val count: Int) : MessageQueue.IdleHandler {
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

}