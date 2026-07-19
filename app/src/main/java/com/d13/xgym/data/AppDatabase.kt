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
    version = 2,
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

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "xgym.db"
                )
                    .addMigrations(MIGRATION_1_2)
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
                    Category(1, "Pecho / Hombro / Tríceps"),
                    Category(2, "Espalda / Bíceps"),
                    Category(3, "Pierna"),
                    Category(4, "Cardio")
                )
            )
            dao.insertSubcategories(
                listOf(
                    Subcategory(11, 1, "Pecho"),
                    Subcategory(12, 1, "Hombro"),
                    Subcategory(13, 1, "Tríceps"),
                    Subcategory(21, 2, "Espalda"),
                    Subcategory(22, 2, "Bíceps"),
                    Subcategory(31, 3, "Pierna"),
                    Subcategory(41, 4, "Cardio")
                )
            )
            dao.insertExercises(
                listOf(
                    Exercise(subcategoryId = 11, name = "Press banca"),
                    Exercise(subcategoryId = 11, name = "Press inclinado"),
                    Exercise(subcategoryId = 11, name = "Aperturas"),
                    Exercise(subcategoryId = 12, name = "Press militar"),
                    Exercise(subcategoryId = 12, name = "Elevaciones laterales"),
                    Exercise(subcategoryId = 13, name = "Extensión en polea"),
                    Exercise(subcategoryId = 13, name = "Fondos"),
                    Exercise(subcategoryId = 21, name = "Jalón al pecho"),
                    Exercise(subcategoryId = 21, name = "Remo con barra"),
                    Exercise(subcategoryId = 22, name = "Curl con barra"),
                    Exercise(subcategoryId = 22, name = "Curl martillo"),
                    Exercise(subcategoryId = 31, name = "Sentadilla"),
                    Exercise(subcategoryId = 31, name = "Prensa"),
                    Exercise(subcategoryId = 31, name = "Peso muerto"),
                    Exercise(subcategoryId = 41, name = "Caminadora"),
                    Exercise(subcategoryId = 41, name = "Elíptica"),
                    Exercise(subcategoryId = 41, name = "Bicicleta")
                )
            )
        }
    }
}
