package com.rifttally.core.data.legend

import android.util.Log
import javax.inject.Inject

/** Reports catalog problems. Abstracted so the repository can be unit-tested without Android. */
internal fun interface LegendLogger {
    fun warn(message: String, throwable: Throwable?)
}

internal class AndroidLegendLogger @Inject constructor() : LegendLogger {
    override fun warn(message: String, throwable: Throwable?) {
        Log.w(TAG, message, throwable)
    }

    private companion object {
        const val TAG = "LegendRepository"
    }
}
