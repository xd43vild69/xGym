package com.d13.xgym.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, id ASC")
    fun categories(): Flow<List<Category>>

    @Update
    suspend fun updateCategories(categories: List<Category>)

    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY orderIndex ASC, id ASC")
    fun subcategories(categoryId: Long): Flow<List<Subcategory>>

    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY orderIndex ASC, id ASC")
    suspend fun subcategoriesOnce(categoryId: Long): List<Subcategory>

    @Update
    suspend fun updateSubcategories(subcategories: List<Subcategory>)

    @Query("SELECT * FROM exercises WHERE subcategoryId = :subcategoryId ORDER BY orderIndex ASC, name ASC")
    fun exercises(subcategoryId: Long): Flow<List<Exercise>>

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @androidx.room.Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Insert
    suspend fun insertCategories(items: List<Category>)

    @Insert
    suspend fun insertSubcategories(items: List<Subcategory>)

    @Insert
    suspend fun insertExercises(items: List<Exercise>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int
}

data class SessionWithCategory(
    @Embedded val session: Session,
    val categoryName: String,
    val setCount: Int
)

data class SetWithExercise(
    @Embedded val set: SetRecord,
    val exerciseName: String
)

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: Long): Session?

    @Insert
    suspend fun insertSet(set: SetRecord): Long

    @Update
    suspend fun updateSet(set: SetRecord)

    @Query(
        """SELECT s.*, c.name AS categoryName,
           (SELECT COUNT(*) FROM set_records r WHERE r.sessionId = s.id) AS setCount
           FROM sessions s JOIN categories c ON c.id = s.categoryId
           ORDER BY s.startTs DESC"""
    )
    fun sessionsWithCategory(): Flow<List<SessionWithCategory>>

    @Query(
        """SELECT s.*, c.name AS categoryName, 
           (SELECT COUNT(*) FROM set_records r WHERE r.sessionId = s.id) AS setCount
           FROM sessions s JOIN categories c ON c.id = s.categoryId
           WHERE s.id = :id"""
    )
    suspend fun sessionWithCategory(id: Long): SessionWithCategory?

    @Query(
        """SELECT r.*, e.name AS exerciseName
           FROM set_records r JOIN exercises e ON e.id = r.exerciseId
           WHERE r.sessionId = :sessionId
           ORDER BY r.exerciseStartTs"""
    )
    suspend fun setsForSession(sessionId: Long): List<SetWithExercise>

    @Query("DELETE FROM sessions WHERE date = :date")
    suspend fun deleteSessionsByDate(date: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @androidx.room.Delete
    suspend fun deleteSession(session: Session)

    @Query("UPDATE sessions SET endTs = :now, durationMs = (:now - startTs) WHERE endTs IS NULL AND startTs < :threshold")
    suspend fun closeStaleSessions(now: Long, threshold: Long)

    @Query("SELECT * FROM sessions WHERE endTs IS NULL ORDER BY startTs DESC LIMIT 1")
    suspend fun getActiveSession(): Session?
}
