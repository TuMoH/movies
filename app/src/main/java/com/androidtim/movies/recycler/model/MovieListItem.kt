package com.androidtim.movies.recycler.model

import com.androidtim.movies.model.MovieItem

data class MovieListItem(
        val movieItem: MovieItem,
        var isMenuOpen: Boolean = false
)