package com.companyname.primeornot.data

/**
 * Compact, dependency-free encoding for the two lists that do not fit into
 * scalar Preferences keys. Every decoder is tolerant: a malformed line or a
 * truncated session is skipped instead of crashing the app.
 *
 * Completed game: id|completedAt|score|correct|incorrect|bestStreak
 * Active session:  id|card|next|score|streak|best|seen|correct|incorrect|
 *                  remaining|limit|bestAtStart|fastest|newBest|used,used,...
 */
internal object RecordCodec {

    private const val FIELD = '|'
    private const val LINE = '\n'
    private const val SESSION_FIELDS = 15

    fun encodeRecent(games: List<CompletedGame>): String =
        games.take(MAX_RECENT_GAMES).joinToString(LINE.toString()) { game ->
            listOf(
                game.id,
                game.completedAtMillis.toString(),
                game.score.toString(),
                game.correct.toString(),
                game.incorrect.toString(),
                game.bestStreak.toString(),
            ).joinToString(FIELD.toString())
        }

    fun decodeRecent(raw: String?): List<CompletedGame> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence()
            .mapNotNull(::decodeGameLine)
            .take(MAX_RECENT_GAMES)
            .toList()
    }

    private fun decodeGameLine(line: String): CompletedGame? {
        val parts = line.split(FIELD)
        if (parts.size != 6) return null
        return runCatching {
            CompletedGame(
                id = parts[0],
                completedAtMillis = parts[1].toLong(),
                score = parts[2].toInt().coerceAtLeast(0),
                correct = parts[3].toInt().coerceAtLeast(0),
                incorrect = parts[4].toInt().coerceAtLeast(0),
                bestStreak = parts[5].toInt().coerceAtLeast(0),
            )
        }.getOrNull()
    }

    fun encodeSession(session: ActiveClassicSession): String =
        listOf(
            session.id,
            session.cardValue.toString(),
            session.nextCardValue.toString(),
            session.score.toString(),
            session.streak.toString(),
            session.bestStreak.toString(),
            session.cardsSeen.toString(),
            session.correct.toString(),
            session.incorrect.toString(),
            session.remainingMs.toString(),
            session.timeLimitMs.toString(),
            session.bestAtRunStart.toString(),
            (session.fastestAnswerMs ?: -1L).toString(),
            if (session.isNewBest) "1" else "0",
            session.usedValues.joinToString(","),
        ).joinToString(FIELD.toString())

    fun decodeSession(raw: String?): ActiveClassicSession? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(FIELD)
        if (parts.size != SESSION_FIELDS) return null
        return runCatching {
            ActiveClassicSession(
                id = parts[0].ifBlank { return null },
                cardValue = parts[1].toInt(),
                nextCardValue = parts[2].toInt(),
                score = parts[3].toInt().coerceAtLeast(0),
                streak = parts[4].toInt().coerceAtLeast(0),
                bestStreak = parts[5].toInt().coerceAtLeast(0),
                cardsSeen = parts[6].toInt().coerceAtLeast(0),
                correct = parts[7].toInt().coerceAtLeast(0),
                incorrect = parts[8].toInt().coerceAtLeast(0),
                remainingMs = parts[9].toLong().coerceAtLeast(0L),
                timeLimitMs = parts[10].toLong().coerceAtLeast(1L),
                bestAtRunStart = parts[11].toInt().coerceAtLeast(0),
                fastestAnswerMs = parts[12].toLong().takeIf { it >= 0L },
                isNewBest = parts[13] == "1",
                usedValues = parts[14]
                    .split(',')
                    .mapNotNull { it.toIntOrNull() },
            )
        }.getOrNull()
    }
}
