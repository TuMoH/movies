package com.androidtim.movies.recycler

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.androidtim.movies.R
import com.androidtim.movies.recycler.model.GenreListItem

class HeaderViewHolder(view: View, onClickListener: OnHeaderClickListener?) :
        RecyclerView.ViewHolder(view) {

    val title: TextView = view.findViewById(R.id.title)
    val arrow: ImageView = view.findViewById(R.id.arrow)

    companion object {
        fun create(inflater: LayoutInflater,
                   parent: ViewGroup,
                   onClickListener: OnHeaderClickListener?
        ): HeaderViewHolder {
            val view = inflater.inflate(R.layout.list_header, parent, false)
            return HeaderViewHolder(view, onClickListener)
        }
    }

    init {
        itemView.setOnClickListener {
            if (onClickListener != null) {
                val item = getItem()
                val toRotation = if (item.expanded) 0f else 180f
                arrow.animate().cancel()
                arrow.animate().rotation(toRotation).start()

                onClickListener(adapterPosition, item)
            }
        }
    }

    fun bind(item: GenreListItem) {
        itemView.setTag(R.id.genre_list_item_key, item)

        title.text = item.title
        arrow.rotation = if (item.expanded) 180f else 0f
    }

    private fun getItem(): GenreListItem {
        return itemView.getTag(R.id.genre_list_item_key) as GenreListItem
    }
}