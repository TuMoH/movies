package com.androidtim.movies.recycler

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.androidtim.movies.R
import com.androidtim.movies.recycler.viewmodel.GenreListItem
import com.androidtim.movies.recycler.viewmodel.MovieListItem
import com.androidtim.movies.recycler.swipe.SwipeHorizontalLayout

class ItemViewHolder(view: View,
                     private val onItemDismissListener: OnItemDismissListener?,
                     private val onItemMenuClickListener: OnItemMenuClickListener?) :
        RecyclerView.ViewHolder(view) {

    companion object {
        private var THIS_IS_FIRST_ITEM = true

        fun create(inflater: LayoutInflater,
                   parent: ViewGroup?,
                   onItemDismissListener: OnItemDismissListener?,
                   onItemMenuClickListener: OnItemMenuClickListener?
        ): ItemViewHolder {
            val view = inflater.inflate(R.layout.list_item, parent, false)
            return ItemViewHolder(view, onItemDismissListener, onItemMenuClickListener)
        }
    }

    val swipeLayout: SwipeHorizontalLayout = view.findViewById(R.id.swipe_layout)
    val title: TextView = view.findViewById(R.id.title)
    val year: TextView = view.findViewById(R.id.year)
    val runtime: TextView = view.findViewById(R.id.runtime)
    val director: TextView = view.findViewById(R.id.director)
    val menu: View = view.findViewById(R.id.swipe_view_menu)

    init {
        swipeLayout.swipeCallback = SwipeCallback()
        menu.setOnClickListener {
            onItemMenuClickListener?.invoke(adapterPosition, getGenreListItem(), getMovieListItem())
        }
    }

    fun bind(genreListItem: GenreListItem, movieListItem: MovieListItem) {
        itemView.setTag(R.id.movie_list_item_key, movieListItem)
        itemView.setTag(R.id.genre_list_item_key, genreListItem)

        val movie = movieListItem.movie
        title.text = movie.title
        year.text = movie.year
        runtime.text = runtime.context.getString(R.string.runtime, movie.runtime)
        director.text = movie.director

        if (movieListItem.isMenuOpen) {
            swipeLayout.openMenu()
        } else {
            swipeLayout.closeMenu()
        }

        if (THIS_IS_FIRST_ITEM) {
            THIS_IS_FIRST_ITEM = false
            swipeLayout.checkAndShowTip()
        }
    }

    fun getGenreListItem(): GenreListItem {
        return itemView.getTag(R.id.genre_list_item_key) as GenreListItem
    }

    fun getMovieListItem(): MovieListItem {
        return itemView.getTag(R.id.movie_list_item_key) as MovieListItem
    }


    inner class SwipeCallback : SwipeHorizontalLayout.SwipeCallback {
        override fun onSwipeChanged(translationX: Int) {
            menu.translationX = (translationX / 2).toFloat() + menu.width / 2
        }

        override fun onMenuOpened() {
            getMovieListItem().isMenuOpen = true
        }

        override fun onMenuClosed() {
            getMovieListItem().isMenuOpen = false
        }

        override fun onSwipedOut() {
            onItemDismissListener?.invoke(adapterPosition, getGenreListItem(), getMovieListItem())
        }
    }

}