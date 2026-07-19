package com.d13.xgym.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.d13.xgym.services.WorkoutService
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
    /** Milisegundos transcurridos en total para la sesión, nunca se pausa */
    val sessionElapsedMs: Long = 0,
    /** Duración del descanso en ms (se cachea al iniciar descanso para evitar leer disco) */
    val restDurationMs: Long = 0L,
    /** Reps sugeridas para la serie recién terminada (serie previa del mismo ejercicio; 10 por defecto) */
    val suggestedReps: Int = 10
)

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val workoutDao = db.workoutDao()
    val catalogDao = db.catalogDao()
    private val prefs = Preferences(app)

    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val threshold = now - 12 * 60 * 60 * 1000L
            workoutDao.closeStaleSessions(now, threshold)
            
            val activeSession = workoutDao.getActiveSession()
            if (activeSession != null) {
                val active = prefs.activeExercise
                if (active != null) {
                    // Había una serie en curso al perder foco: restaurar EXERCISING
                    // para que el usuario pueda terminarla donde la dejó.
                    _ui.update {
                        it.copy(
                            sessionId = activeSession.id,
                            categoryId = active.categoryId,
                            subcategoryId = active.subcategoryId,
                            sessionStartTs = activeSession.startTs,
                            phase = Phase.EXERCISING,
                            phaseStartTs = active.phaseStartTs,
                            exerciseId = active.exerciseId,
                            exerciseName = active.exerciseName,
                            setNumber = active.setNumber
                        )
                    }
                } else {
                    val sets = workoutDao.setsForSession(activeSession.id)
                    val lastSet = sets.lastOrNull()
                    _ui.update {
                        it.copy(
                            sessionId = activeSession.id,
                            categoryId = activeSession.categoryId,
                            sessionStartTs = activeSession.startTs,
                            phase = Phase.IDLE,
                            exerciseId = lastSet?.set?.exerciseId,
                            exerciseName = lastSet?.exerciseName ?: "",
                            setNumber = lastSet?.set?.setNumber ?: 0
                        )
                    }
                }
            } else {
                // No hay sesión activa: descartar cualquier marcador obsoleto.
                prefs.activeExercise = null
            }

            while (true) {
                if (_ui.value.phase != Phase.IDLE || _ui.value.sessionStartTs != null) {
                    _ui.update {
                        val newElapsed = if (it.phase == Phase.IDLE) 0 else System.currentTimeMillis() - it.phaseStartTs
                        // Dispara una sola vez por descanso: restAlarmFired queda en true
                        // hasta que se sale de la fase RESTING.
                        val fireNow = it.phase == Phase.RESTING && newElapsed >= it.restDurationMs && !it.restAlarmFired
                        
                        // Cronómetro de sesión total continuo e ininterrumpido
                        val sessionTotalMs = if (it.sessionStartTs != null) {
                            System.currentTimeMillis() - it.sessionStartTs
                        } else {
                            0
                        }
                        it.copy(
                            elapsedMs = newElapsed,
                            showRestAlarm = if (it.phase == Phase.RESTING) fireNow || it.showRestAlarm else false,
                            restAlarmFired = if (it.phase == Phase.RESTING) it.restAlarmFired || fireNow else false,
                            sessionElapsedMs = sessionTotalMs
                        )
                    }
                }
                delay(200)
            }
        }
    }

    /** Persiste el ejercicio en curso (fase EXERCISING) para poder recuperarlo. */
    private fun persistActiveExercise() {
        val s = _ui.value
        val exId = s.exerciseId
        val catId = s.categoryId
        if (s.phase == Phase.EXERCISING && exId != null && catId != null) {
            prefs.activeExercise = com.d13.xgym.data.ActiveExercise(
                categoryId = catId,
                subcategoryId = s.subcategoryId ?: -1L,
                exerciseId = exId,
                exerciseName = s.exerciseName,
                setNumber = s.setNumber,
                phaseStartTs = s.phaseStartTs
            )
        }
    }

    /** Limpia el marcador de serie en curso. */
    private fun clearActiveExercise() {
        prefs.activeExercise = null
    }

    private fun sendServiceAction(action: String, extras: (Intent.() -> Unit)? = null) {
        val intent = Intent(getApplication<Application>(), WorkoutService::class.java).apply {
            this.action = action
            extras?.invoke(this)
        }
        if (action == WorkoutService.ACTION_START_WORKOUT || action == WorkoutService.ACTION_START_REST) {
            ContextCompat.startForegroundService(getApplication<Application>(), intent)
        } else {
            getApplication<Application>().startService(intent)
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
                    setNumber = 0,
                    phase = Phase.IDLE,
                    elapsedMs = 0
                )
            }
            clearActiveExercise()
        }
    }

    /** Reordena la lista de ejercicios actualizando el orderIndex de cada uno. */
    fun reorderExercises(exercises: List<com.d13.xgym.data.Exercise>) {
        viewModelScope.launch {
            val updated = exercises.mapIndexed { index, ex ->
                ex.copy(orderIndex = index)
            }
            catalogDao.updateExercises(updated)
        }
    }

    /** Reordena la lista de subcategorías actualizando el orderIndex de cada una. */
    fun reorderSubcategories(subcategories: List<com.d13.xgym.data.Subcategory>) {
        viewModelScope.launch {
            val updated = subcategories.mapIndexed { index, sub ->
                sub.copy(orderIndex = index)
            }
            catalogDao.updateSubcategories(updated)
        }
    }

    /** Reordena la lista de categorías actualizando el orderIndex de cada una. */
    fun reorderCategories(categories: List<com.d13.xgym.data.Category>) {
        viewModelScope.launch {
            val updated = categories.mapIndexed { index, cat ->
                cat.copy(orderIndex = index)
            }
            catalogDao.updateCategories(updated)
        }
    }

    /** Elimina un ejercicio de la base de datos. */
    fun deleteExercise(exercise: com.d13.xgym.data.Exercise) {
        viewModelScope.launch {
            catalogDao.deleteExercise(exercise)
        }
    }

    /** Renombra un ejercicio existente. */
    fun renameExercise(exercise: com.d13.xgym.data.Exercise, newName: String) {
        viewModelScope.launch {
            catalogDao.updateExercise(exercise.copy(name = newName))
        }
    }

    fun startSet() {
        val now = System.currentTimeMillis()
        val isFirstSet = _ui.value.sessionStartTs == null
        if (isFirstSet) {
            val sessionId = _ui.value.sessionId ?: return
            viewModelScope.launch {
                workoutDao.session(sessionId)?.let {
                    workoutDao.updateSession(it.copy(startTs = now))
                }
            }
        }
        _ui.update { it ->
            val newSessionStartTs = it.sessionStartTs ?: now
            if (it.sessionStartTs == null) {
                sendServiceAction(WorkoutService.ACTION_START_WORKOUT) {
                    putExtra(WorkoutService.EXTRA_SESSION_START_TS, now)
                }
            }
            it.copy(
                phase = Phase.EXERCISING,
                phaseStartTs = now,
                elapsedMs = 0,
                sessionStartTs = newSessionStartTs
            )
        }
        persistActiveExercise()
    }

    /** Termina la serie: guarda el registro y pasa a descanso. */
    fun endSet() {
        val s = _ui.value
        val exerciseId = s.exerciseId ?: return
        val sessionId = s.sessionId ?: return
        if (s.phase != Phase.EXERCISING) return
        val now = System.currentTimeMillis()
        val completed = s.setNumber + 1
        viewModelScope.launch {
            val setId = workoutDao.insertSet(
                SetRecord(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    setNumber = completed,
                    exerciseStartTs = s.phaseStartTs,
                    exerciseEndTs = now
                )
            )
            // Sugerir las reps de la serie previa del mismo ejercicio en esta sesión.
            val suggested = workoutDao.setsForSession(sessionId)
                .filter { it.set.exerciseId == exerciseId && it.set.reps != null }
                .maxByOrNull { it.set.exerciseEndTs }?.set?.reps ?: 10
            _ui.update {
                it.copy(
                    phase = Phase.RESTING,
                    phaseStartTs = now,
                    elapsedMs = 0,
                    pendingSetId = setId,
                    setNumber = completed,
                    suggestedReps = suggested,
                    restDurationMs = prefs.restDurationSeconds * 1000L
                )
            }
            // La serie ya quedó registrada; ya no hay ejercicio "en curso".
            clearActiveExercise()
            sendServiceAction(WorkoutService.ACTION_START_REST) {
                putExtra(WorkoutService.EXTRA_REST_START_TS, now)
                putExtra(WorkoutService.EXTRA_REST_DURATION_MS, prefs.restDurationSeconds * 1000L)
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
            sendServiceAction(WorkoutService.ACTION_STOP_REST)
            _ui.update { it.copy(phase = Phase.EXERCISING, phaseStartTs = now, elapsedMs = 0) }
            persistActiveExercise()
        }
    }

    fun cancelRest() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            closeOpenRest(now)
            sendServiceAction(WorkoutService.ACTION_STOP_REST)
            _ui.update { it.copy(phase = Phase.IDLE, phaseStartTs = now, elapsedMs = 0) }
            clearActiveExercise()
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
                    setNumber = 0,
                    phase = Phase.IDLE,
                    elapsedMs = 0
                )
            }
            clearActiveExercise()
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
                        setNumber = s.setNumber + 1,
                        exerciseStartTs = s.phaseStartTs,
                        exerciseEndTs = now
                    )
                )
            }
            closeOpenRest(now)
            sendServiceAction(WorkoutService.ACTION_STOP_WORKOUT)
            workoutDao.session(sessionId)?.let {
                workoutDao.updateSession(it.copy(endTs = now, durationMs = s.sessionElapsedMs))
            }
            clearActiveExercise()
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

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            workoutDao.deleteSession(session)
        }
    }

    fun cancelCurrentSession() {
        val s = _ui.value
        val sessionId = s.sessionId ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            closeOpenRest(now)
            sendServiceAction(WorkoutService.ACTION_STOP_WORKOUT)
            workoutDao.session(sessionId)?.let { session ->
                workoutDao.updateSession(session.copy(endTs = now, durationMs = s.sessionElapsedMs))
            }
            clearActiveExercise()
            _ui.value = WorkoutUiState()
        }
    }

    /** Cierra la sesión activa actual (si existe) y comienza una nueva desde cero. */
    fun startNewSession(categoryId: Long, subcategoryId: Long, exerciseId: Long, exerciseName: String) {
        val s = _ui.value
        val sessionId = s.sessionId
        val now = System.currentTimeMillis()
        
        viewModelScope.launch {
            // Terminar sesión previa formalmente
            if (sessionId != null) {
                closeOpenRest(now)
                sendServiceAction(WorkoutService.ACTION_STOP_WORKOUT)
                workoutDao.session(sessionId)?.let { session ->
                    // Si la sesión anterior no duró nada o no tenía series, se podría borrar. 
                    // Pero por ahora solo la terminamos.
                    workoutDao.updateSession(session.copy(endTs = now, durationMs = s.sessionElapsedMs))
                }
            }

            // Iniciar nueva sesión
            val newSessionId = workoutDao.insertSession(
                Session(
                    categoryId = categoryId,
                    date = LocalDate.now().toString(),
                    startTs = now
                )
            )

            _ui.update {
                it.copy(
                    sessionId = newSessionId,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    setNumber = 0,
                    phase = Phase.IDLE,
                    elapsedMs = 0,
                    sessionStartTs = null, // Todavía no presiona "Iniciar serie" de esta nueva
                    sessionElapsedMs = 0
                )
            }
            clearActiveExercise()
        }
    }

    /** Reinicia el servicio en primer plano con el tiempo de inicio guardado. */
    fun resumeWorkoutService() {
        val startTs = _ui.value.sessionStartTs ?: return
        sendServiceAction(WorkoutService.ACTION_START_WORKOUT) {
            putExtra(WorkoutService.EXTRA_SESSION_START_TS, startTs)
        }
    }
}
