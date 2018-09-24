package com.androidtim.movies.recycler.swipe

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager

class TipHelper(context: Context) {

    companion object {
        private const val TIP_SHOWED_KEY = "TIP_SHOWED_KEY"
        private const val SHOW_DELAY = 1000L
    }

    private var handler = Handler(Looper.getMainLooper())
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var tipCallback: Runnable? = null
    private var tipShowed = false

    init {
        tipShowed = sharedPreferences.getBoolean(TIP_SHOWED_KEY, false)
    }

    fun checkAndShowDelayedTip(animation: () -> Unit) {
        if (tipShowed || tipCallback != null) return

        tipCallback = Runnable {
            animation()

            setShowed()
            tipCallback = null
        }
        handler.postDelayed(tipCallback, SHOW_DELAY)
    }

    fun cancel(forever: Boolean = false) {
        if (tipShowed) return

        if (tipCallback != null) {
            handler.removeCallbacks(tipCallback)
            tipCallback = null
        }
        if (forever) {
            setShowed()
        }
    }

    private fun setShowed() {
        tipShowed = true
        sharedPreferences.edit().putBoolean(TIP_SHOWED_KEY, true).apply()
    }

}