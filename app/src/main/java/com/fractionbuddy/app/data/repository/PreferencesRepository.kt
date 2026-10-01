package com.fractionbuddy.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.domain.generation.Difficulty
import com.fractionbuddy.app.domain.generation.TopicSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fractionbuddy_prefs")

data class AppPreferences(
    val defaultTopic: TopicSelection = TopicSelection.MIXED,
    val defaultDifficulty: Difficulty = Difficulty.EASY,
    val preferredModel: ModelType = ModelType.CIRCLE,
    val reducedMotion: Boolean = false,
)

/** Fractions last shown on the workbench, restored on launch. */
data class WorkbenchFractions(
    val explore: Fraction = Fraction(1, 2),
    val compareA: Fraction = Fraction(1, 2),
    val compareB: Fraction = Fraction(1, 3),
    val addA: Fraction = Fraction(1, 2),
    val addB: Fraction = Fraction(1, 4),
)

class PreferencesRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val topic = stringPreferencesKey("default_topic")
        val difficulty = stringPreferencesKey("default_difficulty")
        val model = stringPreferencesKey("preferred_model")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
        val calcDraft = stringPreferencesKey("calculator_draft")
        fun num(slot: String) = intPreferencesKey("${slot}_num")
        fun den(slot: String) = intPreferencesKey("${slot}_den")
    }

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val preferences: Flow<AppPreferences> = data.map { p ->
        AppPreferences(
            defaultTopic = p[Keys.topic].toEnum(TopicSelection.MIXED),
            defaultDifficulty = p[Keys.difficulty].toEnum(Difficulty.EASY),
            preferredModel = p[Keys.model].toEnum(ModelType.CIRCLE),
            reducedMotion = p[Keys.reducedMotion] ?: false,
        )
    }

    val workbench: Flow<WorkbenchFractions> = data.map { p ->
        val d = WorkbenchFractions()
        WorkbenchFractions(
            explore = p.fraction("explore", d.explore),
            compareA = p.fraction("compare_a", d.compareA),
            compareB = p.fraction("compare_b", d.compareB),
            addA = p.fraction("add_a", d.addA),
            addB = p.fraction("add_b", d.addB),
        )
    }

    val calculatorDraft: Flow<String?> = data.map { it[Keys.calcDraft] }

    suspend fun setDefaultTopic(t: TopicSelection) = store.edit { it[Keys.topic] = t.name }
    suspend fun setDefaultDifficulty(d: Difficulty) = store.edit { it[Keys.difficulty] = d.name }
    suspend fun setPreferredModel(m: ModelType) = store.edit { it[Keys.model] = m.name }
    suspend fun setReducedMotion(v: Boolean) = store.edit { it[Keys.reducedMotion] = v }
    suspend fun setCalculatorDraft(draft: String) = store.edit { it[Keys.calcDraft] = draft }

    suspend fun setFraction(slot: WorkbenchSlot, f: Fraction) = store.edit {
        it[Keys.num(slot.key)] = f.numerator
        it[Keys.den(slot.key)] = f.denominator
    }

    suspend fun clearCalculatorDraft() = store.edit { it.remove(Keys.calcDraft) }

    suspend fun clearAll() = store.edit { it.clear() }

    private fun Preferences.fraction(slot: String, default: Fraction): Fraction {
        val n = this[Keys.num(slot)] ?: return default
        val d = this[Keys.den(slot)] ?: return default
        return if (Fraction.isValidLearningInput(n, d)) Fraction(n, d) else default
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
}

enum class WorkbenchSlot(val key: String) {
    EXPLORE("explore"), COMPARE_A("compare_a"), COMPARE_B("compare_b"), ADD_A("add_a"), ADD_B("add_b"),
}
