package com.rifttally.core.data.game

import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.RandomSource
import com.rifttally.core.game.pickFirstPlayer
import com.rifttally.core.model.GameConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Randomly picks who goes first. Every way of creating a game (Setup, Restart, ...) passes
 * [openingEvents] to [GameRepository.createGame] / [GameRepository.restartGame], so the pick is part of
 * the new game from the start: undoing it doesn't make the tracker pick again.
 */
public class FirstPlayerPicker @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val random: RandomSource,
    private val eventFactory: GameEventFactory,
) {
    /** A pick over all of [config]'s players, or `null` if it has none. */
    public fun pick(config: GameConfig): GameEvent.FirstPlayerChosen? =
        pickFirstPlayer(config, random)?.let(eventFactory::firstPlayerChosen)

    /** The events a new game starts with: a pick when "Pick first player at game start" is on. */
    public suspend fun openingEvents(config: GameConfig): List<GameEvent> =
        if (settingsRepository.userSettings.first().pickFirstPlayer) listOfNotNull(pick(config)) else emptyList()
}
