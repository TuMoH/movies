package com.androidtim.movies.recycler.model

import com.androidtim.movies.model.Movie

data class MovieListItem(
        val movie: Movie,
        var isMenuOpen: Boolean = false
)