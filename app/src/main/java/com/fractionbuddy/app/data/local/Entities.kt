package com.fractionbuddy.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A successful basic-calculator calculation. Decimal values are canonical strings. */
@Entity(tableName = "calculations", indices = [Index("createdAt")])
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leftOperand: String,
    val rightOperand: String,
    val operator: String,
    val result: String,
    val rounded: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "practice_sessions", indices = [Index("status"), Index("startedAt")])
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicSelection: String,
    val difficulty: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    /** ACTIVE, COMPLETED or ENDED_EARLY. */
    val status: String,
    /** Denormalised first-answer counts, updated in the same transaction as each answer. */
    @ColumnInfo(defaultValue = "0") val answeredCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val correctCount: Int = 0,
)

@Entity(
    tableName = "fraction_questions",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index(value = ["sessionId", "position"], unique = true)],
)
data class FractionQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val position: Int,
    val topic: String,
    val firstNumerator: Int,
    val firstDenominator: Int,
    val secondNumerator: Int? = null,
    val secondDenominator: Int? = null,
    /** Canonical code: "3/4" or A / B / EQUAL. */
    val correctAnswer: String,
    /** Canonical codes joined with ','. */
    val answerOptions: String,
    val modelType: String,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val answeredAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val hintUsed: Boolean = false,
)

/**
 * Lifetime totals (single row, id = 1). Kept separately so the 100-session history limit never
 * reduces lifetime progress.
 */
@Entity(tableName = "progress_totals")
data class ProgressTotalsEntity(
    @PrimaryKey val id: Int = 1,
    val answeredCount: Int = 0,
    val correctCount: Int = 0,
    val identifyAnswered: Int = 0,
    val identifyCorrect: Int = 0,
    val compareAnswered: Int = 0,
    val compareCorrect: Int = 0,
    val addAnswered: Int = 0,
    val addCorrect: Int = 0,
    val equivalentAnswered: Int = 0,
    val equivalentCorrect: Int = 0,
)

/** Projection used by the retention policy. */
data class SessionRefRow(
    val id: Long,
    val startedAt: Long,
    val answeredCount: Int,
    val status: String,
)
