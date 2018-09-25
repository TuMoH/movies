package com.androidtim.movies.recycler.viewmodel

import com.androidtim.movies.model.Movie

data class MovieListItem(
        val movie: Movie,
        var isMenuOpen: Boolean = false
)