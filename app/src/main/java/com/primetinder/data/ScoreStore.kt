package com.primetinder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ScoreStore {
    val bestScore: Flow<Int>
    suspend fun recordScore(score: Int)
}

private val Context.scoreDataStore by preferencesDataStore(name = "prime_tinder_scores")

class DataStoreScoreStore(private val context: Context) : ScoreStore {

    override val bestScore: Flow<Int> =
        context.scoreDataStore.data.map { preferences -> preferences[BestScoreKey] ?: 0 }

    override suspend fun recordScore(score: Int) {
        context.scoreDataStore.edit { preferences ->
            if (score > (preferences[BestScoreKey] ?: 0)) {
                preferences[BestScoreKey] = score
            }
        }
    }

    private companion object {
        val BestScoreKey = intPreferencesKey("best_score")
    }
}
