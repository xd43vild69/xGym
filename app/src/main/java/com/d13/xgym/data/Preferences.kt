package com.d13.xgym.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Plan de un día del plan semanal.
 * - [rest] = true → día de descanso (exclusivo, sin categorías).
 * - [categoryIds] vacío y [rest] = false → día sin asignar.
 * - [categoryIds] con IDs → día de entrenamiento con una o varias categorías.
 */
data class DayPlan(val rest: Boolean = false, val categoryIds: List<Long> = emptyList())

class Preferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("xgym_prefs", Context.MODE_PRIVATE)

    var restDurationSeconds: Int
        get() = prefs.getInt("rest_duration_seconds", 90)
        set(value) = prefs.edit().putInt("rest_duration_seconds", value).apply()

    /**
     * Plan semanal: 7 días, índice 0=Lunes … 6=Domingo.
     * Serializado como 7 segmentos separados por ";". Cada segmento:
     * "R" = descanso, "" = sin asignar, "1,2,3" = IDs de categorías.
     * Ausente => semana vacía (estado por defecto).
     */
    var weeklyPlan: List<DayPlan>
        get() {
            val raw = prefs.getString("weekly_plan_v2", null) ?: return List(7) { DayPlan() }
            val segs = raw.split(";")
            return List(7) { i ->
                val seg = segs.getOrNull(i)?.trim().orEmpty()
                when {
                    seg == "R" -> DayPlan(rest = true)
                    seg.isEmpty() -> DayPlan()
                    else -> DayPlan(categoryIds = seg.split(",").mapNotNull { it.toLongOrNull() })
                }
            }
        }
        set(value) = prefs.edit()
            .putString("weekly_plan_v2", value.take(7).joinToString(";") { d ->
                when {
                    d.rest -> "R"
                    d.categoryIds.isEmpty() -> ""
                    else -> d.categoryIds.joinToString(",")
                }
            }).apply()
}
