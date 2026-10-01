package com.fractionbuddy.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        CalculationEntity::class,
        PracticeSessionEntity::class,
        FractionQuestionEntity::class,
        ProgressTotalsEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao
    abstract fun practiceDao(): PracticeDao

    companion object {
        const val VERSION = 1
        private const val NAME = "fractionbuddy.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                // No destructive fallback: user progress must survive app updates.
                .build()
    }
}

/**
 * Schema migrations. Version 1 is the initial schema (exported to app/schemas/).
 * Every future schema change must bump [AppDatabase.VERSION] and add a Migration here, e.g.
 *
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE ...") }
 * }
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
