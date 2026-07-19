package com.d13.xgym.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Category::class, Subcategory::class, Exercise::class, Session::class, SetRecord::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sessions ADD COLUMN durationMs INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE subcategories ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Limpia categorías y subcategorías para replantear con nuevas
                database.execSQL("DELETE FROM exercises")
                database.execSQL("DELETE FROM subcategories")
                database.execSQL("DELETE FROM categories")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "xgym.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also {
                    instance = it
                    CoroutineScope(Dispatchers.IO).launch { seedIfEmpty(it) }
                }
            }

        private suspend fun seedIfEmpty(db: AppDatabase) {
            val dao = db.catalogDao()
            if (dao.categoryCount() > 0) return
            dao.insertCategories(
                listOf(
                    Category(1, "Pecho"),
                    Category(2, "Hombro"),
                    Category(3, "Tríceps"),
                    Category(4, "Espalda"),
                    Category(5, "Bíceps"),
                    Category(6, "Pierna"),
                    Category(7, "Cardio"),
                    Category(8, "Core")
                )
            )
            dao.insertSubcategories(
                listOf(
                    Subcategory(1, 1, "Pecho"),
                    Subcategory(2, 2, "Hombro"),
                    Subcategory(3, 3, "Tríceps"),
                    Subcategory(4, 4, "Espalda"),
                    Subcategory(5, 5, "Bíceps"),
                    Subcategory(6, 6, "Pierna"),
                    Subcategory(7, 7, "Cardio"),
                    Subcategory(8, 8, "Core")
                )
            )
            dao.insertExercises(
                listOf(
                    // Pecho
                    Exercise(subcategoryId = 1, name = "Press banca"),
                    Exercise(subcategoryId = 1, name = "Press inclinado"),
                    Exercise(subcategoryId = 1, name = "Aperturas"),
                    // Hombro
                    Exercise(subcategoryId = 2, name = "Press militar"),
                    Exercise(subcategoryId = 2, name = "Elevaciones laterales"),
                    Exercise(subcategoryId = 2, name = "Pájaros"),
                    // Tríceps
                    Exercise(subcategoryId = 3, name = "Extensión en polea"),
                    Exercise(subcategoryId = 3, name = "Fondos"),
                    Exercise(subcategoryId = 3, name = "Extensión con mancuerna"),
                    // Espalda
                    Exercise(subcategoryId = 4, name = "Jalón al pecho"),
                    Exercise(subcategoryId = 4, name = "Remo con barra"),
                    Exercise(subcategoryId = 4, name = "Remo máquina"),
                    // Bíceps
                    Exercise(subcategoryId = 5, name = "Curl con barra"),
                    Exercise(subcategoryId = 5, name = "Curl martillo"),
                    Exercise(subcategoryId = 5, name = "Curl máquina"),
                    // Pierna
                    Exercise(subcategoryId = 6, name = "Sentadilla"),
                    Exercise(subcategoryId = 6, name = "Prensa"),
                    Exercise(subcategoryId = 6, name = "Peso muerto"),
                    Exercise(subcategoryId = 6, name = "Extensión de pierna"),
                    Exercise(subcategoryId = 6, name = "Curl femoral"),
                    // Cardio
                    Exercise(subcategoryId = 7, name = "Caminadora"),
                    Exercise(subcategoryId = 7, name = "Elíptica"),
                    Exercise(subcategoryId = 7, name = "Bicicleta"),
                    Exercise(subcategoryId = 7, name = "Remo máquina"),
                    // Core
                    Exercise(subcategoryId = 8, name = "Abdominales"),
                    Exercise(subcategoryId = 8, name = "Planchas"),
                    Exercise(subcategoryId = 8, name = "Crunch máquina"),
                    Exercise(subcategoryId = 8, name = "Levantamiento de piernas")
                )
            )
        }
    }
}
