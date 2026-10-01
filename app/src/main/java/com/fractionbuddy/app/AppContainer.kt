package com.fractionbuddy.app

import android.app.Application
import android.content.Context
import com.fractionbuddy.app.data.local.AppDatabase
import com.fractionbuddy.app.data.repository.CalculatorRepository
import com.fractionbuddy.app.data.repository.PracticeRepository
import com.fractionbuddy.app.data.repository.PreferencesRepository
import com.fractionbuddy.app.domain.Clock
import com.fractionbuddy.app.domain.RandomProvider

/** Manual dependency injection. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.System
    val randomProvider: RandomProvider = RandomProvider.Default
    private val database: AppDatabase by lazy { AppDatabase.create(context) }
    val preferences: PreferencesRepository by lazy { PreferencesRepository(context) }
    val practice: PracticeRepository by lazy { PracticeRepository(database, clock) }
    val calculator: CalculatorRepository by lazy { CalculatorRepository(database, clock) }

    /** Clears every locally stored record and preference. */
    suspend fun clearAllData() {
        calculator.clear()
        practice.clearProgress()
        preferences.clearAll()
    }
}

class FractionBuddyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
