package com.androidtim.movies

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import com.androidtim.movies.recycler.model.GenreListItem
import com.androidtim.movies.recycler.MovieAdapter
import com.androidtim.movies.recycler.model.MovieListItem

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MovieAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private var listData = ArrayList<GenreListItem>()
    private var loader: MoviesLoader? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutManager = LinearLayoutManager(this)
        recycler_view.layoutManager = layoutManager

        adapter = MovieAdapter(listData, LayoutInflater.from(this))
        adapter.onItemDismissListener = this::removeItem
        adapter.onItemItemMenuClickListener = this::removeItem
        adapter.onHeaderClickListener = this::onClickHeader
        recycler_view.adapter = adapter

        loader = MoviesLoader(this,
                onSuccess = onDataLoaded,
                onError = onDataLoadError)
        loader!!.execute()
    }

    override fun onStop() {
        super.onStop()
        loader?.cancel(true)
        loader = null
    }

    private val onDataLoaded: OnSuccess = { genreList ->
        genreList.forEach { genre ->
            listData.add(GenreListItem(
                    title = genre.title,
                    movies = genre.movies.map { MovieListItem(it) }.toMutableList()
            ))
        }
        adapter.notifyDataSetChanged()
    }

    private val onDataLoadError: OnError = { e ->
        val msg = "data not loaded"
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        Log.e("MainActivity", msg, e)
    }

    private fun removeItem(adapterPosition: Int, genre: GenreListItem, movie: MovieListItem) {
        genre.movies.remove(movie)

        if (genre.movies.isEmpty()) {
            listData.remove(genre)
            adapter.notifyItemRemoved(adapterPosition - 1)
        }
        adapter.notifyItemRemoved(adapterPosition)
    }

    private fun onClickHeader(adapterPosition: Int, genre: GenreListItem) {
        val firstItemPosition = adapterPosition + 1
        if (!genre.expanded) {
            genre.expanded = true
            adapter.notifyItemRangeInserted(firstItemPosition, genre.movies.size)
            recycler_view.smoothScrollToPosition(firstItemPosition)
        } else {
            genre.expanded = false
            adapter.notifyItemRangeRemoved(firstItemPosition, genre.movies.size)
        }
    }

}
