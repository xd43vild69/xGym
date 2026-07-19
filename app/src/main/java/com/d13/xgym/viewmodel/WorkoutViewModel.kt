package com.d13.xgym.viewmodel

import android.app.Application
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d13.xgym.data.AppDatabase
import com.d13.xgym.data.Preferences
import com.d13.xgym.data.Session
import com.d13.xgym.data.SetRecord
import com.d13.xgym.data.SetWithExercise
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Phase { IDLE, EXERCISING, RESTING }

data class WorkoutUiState(
    val phase: Phase = Phase.IDLE,
    val sessionId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val exerciseId: Long? = null,
    val exerciseName: String = "",
    val setNumber: Int = 1,
    /** Inicio de la fase actual (epoch ms), base del cronómetro visible */
    val phaseStartTs: Long = 0,
    /** Milisegundos transcurridos de la fase actual, actualizado por el ticker */
    val elapsedMs: Long = 0,
    /** Serie recién terminada, pendiente de capturar reps */
    val pendingSetId: Long? = null,
    val finishedSets: List<SetWithExercise> = emptyList(),
    /** true cuando se cumple el tiempo de descanso configurado y debe mostrar alerta */
    val showRestAlarm: Boolean = false,
    /** true si la alarma ya se disparó en el descanso actual (no repetir) */
    val restAlarmFired: Boolean = false,
    /** Inicio de la sesión (epoch ms) para el cronómetro total */
    val sessionStartTs: Long? = null,
    /** Milisegundos acumulados de la sesión (pausado) */
    val sessionElapsedPausedMs: Long = 0,
    /** true si la sesión está activa en la pantalla de entrenamiento */
    val sessionActive: Boolean = false
)

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val workoutDao = db.workoutDao()
    val catalogDao = db.catalogDao()
    private val prefs = Preferences(app)
    private val vibrator = app.getSystemService(Vibrator::class.java)

    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _ui.update {
                    val newElapsed = if (it.phase == Phase.IDLE) 0 else System.currentTimeMillis() - it.phaseStartTs
                    val restDurationMs = prefs.restDurationSeconds * 1000L
                    // Dispara una sola vez por descanso: restAlarmFired queda en true
                    // hasta que se sale de la fase RESTING.
                    val fireNow = it.phase == Phase.RESTING && newElapsed >= restDurationMs && !it.restAlarmFired
                    if (fireNow) {
                        vibrator?.vibrate(500)
                    }
                    // Cronómetro de sesión total
                    val sessionTotalMs = if (it.sessionActive && it.sessionStartTs != null) {
                        System.currentTimeMillis() - it.sessionStartTs + it.sessionElapsedPausedMs
                    } else {
                        it.sessionElapsedPausedMs
                    }
                    it.copy(
                        elapsedMs = newElapsed,
                        showRestAlarm = if (it.phase == Phase.RESTING) fireNow || it.showRestAlarm else false,
                        restAlarmFired = if (it.phase == Phase.RESTING) it.restAlarmFired || fireNow else false,
                        sessionElapsedPausedMs = sessionTotalMs.toLong()
                    )
                }
                delay(200)
            }
        }
    }

    /** Crea la sesión (si no existe) y fija el ejercicio activo. */
    fun selectExercise(categoryId: Long, subcategoryId: Long, exerciseId: Long, exerciseName: String) {
        viewModelScope.launch {
            var sessionId = _ui.value.sessionId
            if (sessionId == null) {
                sessionId = workoutDao.insertSession(
                    Session(
                        categoryId = categoryId,
                        date = LocalDate.now().toString(),
                        startTs = System.currentTimeMillis()
                    )
                )
            }
            _ui.update {
                it.copy(
                    sessionId = sessionId,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    setNumber = 1,
                    phase = Phase.IDLE,
                    elapsedMs = 0,
                    sessionActive = true
                )
            }
        }
    }

    fun startSet() {
        val now = System.currentTimeMillis()
        _ui.update { it ->
            val newSessionStartTs = it.sessionStartTs ?: now
            it.copy(
                phase = Phase.EXERCISING,
                phaseStartTs = now,
                elapsedMs = 0,
                sessionStartTs = newSessionStartTs,
                sessionActive = true
            )
        }
    }

    fun pauseSessionTimer() {
        _ui.update { it.copy(sessionActive = false) }
    }

    fun resumeSessionTimer() {
        _ui.update {
            it.copy(
                sessionActive = true,
                sessionStartTs = System.currentTimeMillis() - (it.sessionElapsedPausedMs - it.sessionElapsedPausedMs % 1000)
            )
        }
    }

    /** Termina la serie: guarda el registro y pasa a descanso. */
    fun endSet() {
        val s = _ui.value
        val exerciseId = s.exerciseId ?: return
        val sessionId = s.sessionId ?: return
        if (s.phase != Phase.EXERCISING) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val setId = workoutDao.insertSet(
                SetRecord(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    setNumber = s.setNumber,
                    exerciseStartTs = s.phaseStartTs,
                    exerciseEndTs = now
                )
            )
            _ui.update {
                it.copy(
                    phase = Phase.RESTING,
                    phaseStartTs = now,
                    elapsedMs = 0,
                    pendingSetId = setId,
                    setNumber = it.setNumber + 1
                )
            }
        }
    }

    /** Captura las reps de la serie recién terminada. */
    fun setReps(reps: Int) {
        val setId = _ui.value.pendingSetId ?: return
        viewModelScope.launch {
            val sets = workoutDao.setsForSession(_ui.value.sessionId ?: return@launch)
            sets.firstOrNull { it.set.id == setId }?.let {
                workoutDao.updateSet(it.set.copy(reps = reps))
            }
            _ui.update { it.copy(pendingSetId = null) }
        }
    }

    /** Cierra el descanso actual (registra restEndTs) y arranca la siguiente serie. */
    fun startNextSet() {
        val s = _ui.value
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            closeOpenRest(now)
            _ui.update { it.copy(phase = Phase.EXERCISING, phaseStartTs = now, elapsedMs = 0) }
        }
    }

    /** Cambia de ejercicio manteniendo la sesión. */
    fun changeExercise(subcategoryId: Long, exerciseId: Long, exerciseName: String) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            closeOpenRest(now)
            _ui.update {
                it.copy(
                    subcategoryId = subcategoryId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    setNumber = 1,
                    phase = Phase.IDLE,
                    elapsedMs = 0
                )
            }
        }
    }

    fun finishSession(onDone: (Long) -> Unit) {
        val s = _ui.value
        val sessionId = s.sessionId ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            // Si termina en pleno ejercicio, registra la serie en curso.
            if (s.phase == Phase.EXERCISING && s.exerciseId != null) {
                workoutDao.insertSet(
                    SetRecord(
                        sessionId = sessionId,
                        exerciseId = s.exerciseId,
                        setNumber = s.setNumber,
                        exerciseStartTs = s.phaseStartTs,
                        exerciseEndTs = now
                    )
                )
            }
            closeOpenRest(now)
            workoutDao.session(sessionId)?.let {
                workoutDao.updateSession(it.copy(endTs = now, durationMs = s.sessionElapsedPausedMs))
            }
            _ui.value = WorkoutUiState()
            onDone(sessionId)
        }
    }

    private suspend fun closeOpenRest(now: Long) {
        val s = _ui.value
        if (s.phase == Phase.RESTING) {
            val sets = workoutDao.setsForSession(s.sessionId ?: return)
            sets.maxByOrNull { it.set.exerciseEndTs }?.let {
                if (it.set.restEndTs == null) {
                    workoutDao.updateSet(it.set.copy(restEndTs = now))
                }
            }
        }
    }

    fun dismissRestAlarm() {
        _ui.update { it.copy(showRestAlarm = false) }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            workoutDao.deleteAllSessions()
        }
    }

    fun clearTodaysSessions() {
        viewModelScope.launch {
            workoutDao.deleteSessionsByDate(LocalDate.now().toString())
        }
    }
}
