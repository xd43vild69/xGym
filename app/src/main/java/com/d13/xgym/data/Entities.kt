package com.d13.xgym.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: Long,
    val name: String
)

@Entity(
    tableName = "subcategories",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("categoryId")]
)
data class Subcategory(
    @PrimaryKey val id: Long,
    val categoryId: Long,
    val name: String
)

@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = Subcategory::class,
        parentColumns = ["id"],
        childColumns = ["subcategoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subcategoryId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subcategoryId: Long,
    val name: String
)

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"]
    )],
    indices = [Index("categoryId")]
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    /** Fecha local yyyy-MM-dd de la sesión */
    val date: String,
    val startTs: Long,
    val endTs: Long? = null,
    /** Tiempo total de entrenamiento en milisegundos (ejercicio + descanso) */
    val durationMs: Long? = null
)

@Entity(
    tableName = "set_records",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"]
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SetRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val exerciseStartTs: Long,
    val exerciseEndTs: Long,
    /** Null si la sesión terminó durante el descanso o no hubo descanso */
    val restEndTs: Long? = null,
    val reps: Int? = null
)
