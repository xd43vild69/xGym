package com.d13.xgym.data

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("xgym_prefs", Context.MODE_PRIVATE)

    var restDurationSeconds: Int
        get() = prefs.getInt("rest_duration_seconds", 90)
        set(value) = prefs.edit().putInt("rest_duration_seconds", value).apply()
}
