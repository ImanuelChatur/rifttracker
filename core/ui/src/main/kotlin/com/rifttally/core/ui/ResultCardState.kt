package com.rifttally.core.ui

import androidx.compose.runtime.Immutable
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Legend
import com.rifttally.core.model.SideId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * What a [ResultCard] shows: one finished game.
 *
 * @property endedAt epoch millis, shown as a date.
 * @property sides final results in seat order.
 */
@Immutable
public data class ResultCardState(
    val format: GameFormat,
    val endedAt: Long,
    val sides: ImmutableList<ResultCardSide>,
) {
    /** The winning side, or `null` when the game ended without one. */
    public val winner: ResultCardSide? get() = sides.firstOrNull { it.isWinner }

    /** Every distinct portrait URI on the card, for preloading. */
    public val imageUris: List<String>
        get() = sides.flatMap { side -> side.players.mapNotNull { it.legendImageUri } }.distinct()

    public companion object {
        /**
         * The card for a game with [config], final [scores] per side (missing sides count as 0) and
         * [winner] (one that isn't a side of [config] counts as no winner). Legend names and portraits come
         * from [legendsById]; an unknown Legend shows its id and the fallback avatar.
         */
        public fun of(
            config: GameConfig,
            scores: Map<SideId, Int>,
            winner: SideId?,
            endedAt: Long,
            legendsById: Map<String, Legend>,
        ): ResultCardState {
            val playersById = config.players.associateBy { it.id }
            return ResultCardState(
                format = config.format,
                endedAt = endedAt,
                sides = config.sides.map { side ->
                    ResultCardSide(
                        players = side.playerIds.mapNotNull(playersById::get).map { player ->
                            val legend = player.legendId?.let(legendsById::get)
                            ResultCardPlayer(
                                name = player.name,
                                colorId = player.colorId,
                                legendName = legend?.name ?: player.legendId,
                                legendImageUri = legend?.imageUri,
                            )
                        }.toImmutableList(),
                        score = scores[side.id] ?: 0,
                        isWinner = side.id == winner,
                    )
                }.toImmutableList(),
            )
        }
    }
}

/** One side on a [ResultCard]: its players (two in 2v2, otherwise one), final score and whether it won. */
@Immutable
public data class ResultCardSide(val players: ImmutableList<ResultCardPlayer>, val score: Int, val isWinner: Boolean) {
    /** "Ana", or "Ana & Ben" for a team. */
    public val name: String get() = players.joinToString(TEAM_SEPARATOR) { it.name }
}

/**
 * A player on a [ResultCard]. [colorId] is a `PlayerColors` id (unknown ids fall back to grey);
 * [legendName] is `null` without a Legend and [legendImageUri] `null` without art.
 */
@Immutable
public data class ResultCardPlayer(
    val name: String,
    val colorId: String,
    val legendName: String?,
    val legendImageUri: String?,
)

/** Between teammates' names ("Ana & Ben"), as on the tracker, Home and History. */
internal const val TEAM_SEPARATOR = " & "
