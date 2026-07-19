package com.d13.xgym.data

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("xgym_prefs", Context.MODE_PRIVATE)

    var restDurationSeconds: Int
        get() = prefs.getInt("rest_duration_seconds", 90)
        set(value) = prefs.edit().putInt("rest_duration_seconds", value).apply()

    /**
     * Plan semanal: 7 valores, índice 0=Lunes … 6=Domingo.
     * Código por día: [EMPTY] = sin asignar, [REST] = descanso, > 0 = categoryId.
     * Ausente => semana vacía (estado por defecto).
     */
    var weeklyPlan: List<Int>
        get() {
            val raw = prefs.getString("weekly_plan", null) ?: return List(7) { EMPTY }
            val parts = raw.split(",")
            return List(7) { i -> parts.getOrNull(i)?.toIntOrNull() ?: EMPTY }
        }
        set(value) = prefs.edit()
            .putString("weekly_plan", value.take(7).joinToString(",")).apply()

    companion object {
        const val EMPTY = -1
        const val REST = 0
    }
}
