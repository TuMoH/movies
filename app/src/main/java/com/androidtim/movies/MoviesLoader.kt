package com.androidtim.movies

import android.content.Context
import android.os.AsyncTask
import com.androidtim.movies.model.Genre
import com.androidtim.movies.model.Movie
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

typealias OnSuccess = (List<Genre>) -> Unit
typealias OnError = (Exception) -> Unit

class MoviesLoader(context: Context,
                   onSuccess: OnSuccess,
                   onError: OnError
) : AsyncTask<Unit, Unit, List<Genre>>() {

    companion object {
        private const val ASSET_FILE_NAME = "movies.json"
    }

    private val weakAppContext: WeakReference<Context> = WeakReference(context.applicationContext)
    private val weakOnSuccess: WeakReference<OnSuccess> = WeakReference(onSuccess)
    private val weakOnError: WeakReference<OnError> = WeakReference(onError)

    override fun doInBackground(vararg params: Unit?): List<Genre>? {
        val appContext = weakAppContext.get()
        if (appContext != null) {
            return getGenres(appContext)
        }
        return null
    }

    override fun onPostExecute(result: List<Genre>?) {
        if (result != null) {
            weakOnSuccess.get()?.invoke(result)
        } else {
            weakOnError.get()?.invoke(Exception("unexpected error"))
        }
    }

    private fun getGenres(context: Context): List<Genre> {
        val json = jsonFromAssets(context, ASSET_FILE_NAME)

        val result = ArrayList<Genre>(json.length())
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

    private fun parseGenreItem(json: JSONObject): Genre {
        return Genre(
                title = json.optString("title"),
                movies = parseMovies(json.optJSONArray("movies"))
        )
    }

    private fun parseMovies(json: JSONArray): List<Movie> {
        val result = ArrayList<Movie>(json.length())
        for (i in 0 until json.length()) {
            result.add(parseMovieItem(json.getJSONObject(i)))
        }

        return result
    }

    private fun parseMovieItem(json: JSONObject): Movie {
        return Movie(
                title = json.optString("title"),
                year = json.optString("year"),
                runtime = json.optString("runtime"),
                director = json.optString("director")
        )
    }

}
