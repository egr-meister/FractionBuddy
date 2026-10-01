package com.fractionbuddy.app.data.repository

import androidx.room.withTransaction
import com.fractionbuddy.app.data.local.AppDatabase
import com.fractionbuddy.app.data.local.CalculationEntity
import com.fractionbuddy.app.domain.Clock
import com.fractionbuddy.app.domain.calculator.CalculationRecord
import com.fractionbuddy.app.domain.calculator.Operator
import com.fractionbuddy.app.domain.progress.RetentionPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class HistoryCalculation(
    val id: Long,
    val left: String,
    val operator: Operator,
    val right: String,
    val result: String,
    val rounded: Boolean,
    val createdAt: Long,
)

class CalculatorRepository(private val db: AppDatabase, private val clock: Clock) {
    private val dao = db.calculationDao()

    fun observeHistory(): Flow<List<HistoryCalculation>> =
        dao.observeLatest(RetentionPolicy.MAX_CALCULATIONS).map { list ->
            list.mapNotNull { e ->
                val op = Operator.fromCode(e.operator) ?: return@mapNotNull null
                HistoryCalculation(e.id, e.leftOperand, op, e.rightOperand, e.result, e.rounded, e.createdAt)
            }
        }

    suspend fun add(record: CalculationRecord) {
        db.withTransaction {
            dao.insert(
                CalculationEntity(
                    leftOperand = record.left,
                    rightOperand = record.right,
                    operator = record.operator.code,
                    result = record.result,
                    rounded = record.rounded,
                    createdAt = clock.now(),
                ),
            )
            dao.trimTo(RetentionPolicy.MAX_CALCULATIONS)
        }
    }

    suspend fun clear() = dao.clear()
}
