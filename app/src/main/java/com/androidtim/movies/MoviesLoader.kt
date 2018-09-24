package com.androidtim.movies

import android.content.Context
import android.os.AsyncTask
import com.androidtim.movies.model.GenreItem
import com.androidtim.movies.model.MovieItem
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

typealias OnSuccess = (List<GenreItem>) -> Unit
typealias OnError = (Exception) -> Unit

class MoviesLoader(context: Context,
                   onSuccess: OnSuccess,
                   onError: OnError
) : AsyncTask<Unit, Unit, List<GenreItem>>() {

    private val weakAppContext: WeakReference<Context> = WeakReference(context.applicationContext)
    private val weakOnSuccess: WeakReference<OnSuccess> = WeakReference(onSuccess)
    private val weakOnError: WeakReference<OnError> = WeakReference(onError)

    override fun doInBackground(vararg params: Unit?): List<GenreItem>? {
        val appContext = weakAppContext.get()
        if (appContext != null) {
            return getGenres(appContext)
        }
        return null
    }

    override fun onPostExecute(result: List<GenreItem>?) {
        if (result != null) {
            weakOnSuccess.get()?.invoke(result)
        } else {
            weakOnError.get()?.invoke(Exception("unexpected error"))
        }
    }

    private fun getGenres(context: Context): List<GenreItem> {
        val json = jsonFromAssets(context, "movies.json")

        val result = ArrayList<GenreItem>(json.length())
        for (i in 0 until json.length()) {
            result.add(parseGenreItem(json.getJSONObject(i)))
        }

        return result
    }

    private fun jsonFromAssets(context: Context, fileName: String): JSONArray {
        val input = context.assets.open(fileName)
        val inputAsString = input.bufferedReader().use { it.readText() }
        return JSONArray(inputAsString)
    }

    private fun parseGenreItem(json: JSONObject): GenreItem {
        return GenreItem(
                title = json.optString("title"),
                movies = parseMovies(json.optJSONArray("movies"))
        )
    }

    private fun parseMovies(json: JSONArray): List<MovieItem> {
        val result = ArrayList<MovieItem>(json.length())
        for (i in 0 until json.length()) {
            result.add(parseMovieItem(json.getJSONObject(i)))
        }

        return result
    }

    private fun parseMovieItem(json: JSONObject): MovieItem {
        return MovieItem(
                title = json.optString("title"),
                year = json.optString("year"),
                runtime = json.optString("runtime"),
                director = json.optString("director")
        )
    }

    private fun parseGenres(json: JSONArray): String {
        val sb = StringBuilder()
        for (i in 0 until json.length()) {
            sb.append(json.optString(i))

            if (i < json.length() - 1) {
                sb.append(", ")
            }
        }
        return sb.toString()
    }

}
