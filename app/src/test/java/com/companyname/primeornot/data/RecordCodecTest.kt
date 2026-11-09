package com.companyname.primeornot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordCodecTest {

    private val session = ActiveClassicSession(
        id = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
        cardValue = 91,
        nextCardValue = 997,
        score = 12,
        streak = 3,
        bestStreak = 5,
        cardsSeen = 13,
        correct = 12,
        incorrect = 1,
        remainingMs = 2_400,
        timeLimitMs = 6_000,
        bestAtRunStart = 8,
        fastestAnswerMs = 840,
        isNewBest = true,
        usedValues = listOf(97, 91, 997),
    )

    @Test
    fun activeSessionRoundTrips() {
        assertEquals(session, RecordCodec.decodeSession(RecordCodec.encodeSession(session)))
    }

    @Test
    fun missingFastestAnswerRoundTripsAsNull() {
        val withoutFastest = session.copy(fastestAnswerMs = null)
        val decoded = RecordCodec.decodeSession(RecordCodec.encodeSession(withoutFastest))
        assertEquals(withoutFastest, decoded)
        assertNull(decoded?.fastestAnswerMs)
    }

    @Test
    fun malformedSessionIsDiscardedSafely() {
        assertNull(RecordCodec.decodeSession(null))
        assertNull(RecordCodec.decodeSession(""))
        assertNull(RecordCodec.decodeSession("garbage"))
        assertNull(RecordCodec.decodeSession("||||||||||||||"))
        assertNull(RecordCodec.decodeSession("id|not-a-number|1|2|3|4|5|6|7|8|9|10|11|1|"))
    }

    @Test
    fun recentGamesRoundTripNewestFirst() {
        val games = (1..5).map {
            CompletedGame(
                id = "game-$it",
                completedAtMillis = 1_700_000_000_000L + it,
                score = it,
                correct = it,
                incorrect = 1,
                bestStreak = it,
            )
        }

        assertEquals(games, RecordCodec.decodeRecent(RecordCodec.encodeRecent(games)))
    }

    @Test
    fun recentGamesAreCappedAndBadLinesSkipped() {
        // recentGames is newest first, so the id closest to "now" comes first.
        val games = (30 downTo 1).map {
            CompletedGame(
                id = "game-$it",
                completedAtMillis = it.toLong(),
                score = it,
                correct = it,
                incorrect = 0,
                bestStreak = it,
            )
        }
        val encoded = RecordCodec.encodeRecent(games)

        assertEquals(MAX_RECENT_GAMES, RecordCodec.decodeRecent(encoded).size)
        assertEquals("game-30", RecordCodec.decodeRecent(encoded).first().id)
        assertEquals("game-11", RecordCodec.decodeRecent(encoded).last().id)

        val damaged = "game-1|1|1|1|0|1\nnot a record\n\ngame-2|2|2|2|0|2"
        assertEquals(2, RecordCodec.decodeRecent(damaged).size)
    }

    @Test
    fun accuracyUsesAggregateAnswers() {
        val records = ClassicRecords(totalCorrect = 5, totalIncorrect = 1)
        assertEquals(6, records.totalAnswered)
        assertEquals(83.333, records.accuracyPercent ?: 0.0, 0.001)

        assertNull(ClassicRecords().accuracyPercent)

        val emptyGame = CompletedGame("empty", 0, 0, 0, 0, 0)
        assertNull(emptyGame.accuracyPercent)
        assertTrue(!emptyGame.qualifiesForStreak)
    }
}
