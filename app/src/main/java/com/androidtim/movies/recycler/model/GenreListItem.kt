package com.androidtim.movies.recycler.model

data class GenreListItem(
        val title: String,
        val movies: MutableList<MovieListItem>,
        var expanded: Boolean = false
)