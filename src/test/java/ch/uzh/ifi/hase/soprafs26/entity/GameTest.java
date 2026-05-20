package ch.uzh.ifi.hase.soprafs26.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class GameTest {

    @Test
    void turnAndGamePhaseDefaults_coverNullEnumAndStringBranches() {
        Game game = new Game();

        assertEquals("ROLL_DICE", game.getTurnPhase());
        assertEquals("SETUP", game.getGamePhase());
        assertTrue(game.isSetupPhase());
        assertTrue(game.isFirstSetupRound());
        assertFalse(game.isSecondSetupRound());

        game.setTurnPhase(TurnPhase.ACTION);
        assertEquals("ACTION", game.getTurnPhase());

        game.setTurnPhase((TurnPhase) null);
        assertEquals("ROLL_DICE", game.getTurnPhase());

        game.setGamePhase(GamePhase.ACTIVE);
        assertEquals("ACTIVE", game.getGamePhase());
        assertFalse(game.isSetupPhase());
        assertFalse(game.isFirstSetupRound());
        assertFalse(game.isSecondSetupRound());

        game.setGamePhase((GamePhase) null);
        assertEquals("SETUP", game.getGamePhase());

        game.setGamePhase(" setup_second_round ");
        assertTrue(game.isSetupPhase());
        assertFalse(game.isFirstSetupRound());
        assertTrue(game.isSecondSetupRound());

        game.setGamePhase(" finished ");
        assertFalse(game.isSetupPhase());
    }

    @Test
    void winnerResolution_coverCachedLookupMissingPlayersNullEntriesAndSetterBranches() {
        Player other = player(1L);
        Player winner = player(2L);
        Game game = new Game();
        game.setPlayers(Arrays.asList(null, other, winner));
        game.setWinnerPlayerId(2L);

        assertSame(winner, game.getWinner());
        assertSame(winner, game.getWinner());

        game.setWinner(null);
        assertNull(game.getWinner());
        assertNull(game.getWinnerPlayerId());

        game.setWinner(winner);
        assertSame(winner, game.getWinner());
        assertEquals(2L, game.getWinnerPlayerId());

        Game missingWinner = new Game();
        missingWinner.setPlayers(List.of(other));
        missingWinner.setWinnerPlayerId(99L);
        assertNull(missingWinner.getWinner());

        Game noPlayers = new Game();
        noPlayers.setWinnerPlayerId(99L);
        assertNull(noPlayers.getWinner());
    }

    @Test
    void versionAndTradeTimestamp_coverNullAndIncrementBranches() {
        Game game = new Game();
        Instant requestedAt = Instant.parse("2026-05-20T10:15:30Z");

        game.setTradeRequestedAt(requestedAt);
        assertEquals(requestedAt, game.getTradeRequestedAt());

        game.setGameVersion(null);
        game.incrementGameVersion();
        assertEquals(1L, game.getGameVersion());

        game.incrementGameVersion();
        assertEquals(2L, game.getGameVersion());
    }

    @Test
    void nullablePhaseHelpers_coverDefensiveNullBranches() {
        Game game = new Game() {
            @Override
            public String getGamePhase() {
                return null;
            }
        };

        assertTrue(game.isSetupPhase());
        assertFalse(game.isFirstSetupRound());
        assertFalse(game.isSecondSetupRound());
    }

    @Test
    void transientAndLogFields_coverRemainingAccessors() {
        Game game = new Game();
        Player currentPlayer = player(3L);
        Player longestRoad = player(4L);
        Player largestArmy = player(5L);
        Robber robber = new Robber();
        Dice dice = new Dice();
        DevelopmentDeck developmentDeck = new DevelopmentDeck();

        game.setCurrentPlayer(currentPlayer);
        game.setRobber(robber);
        game.setDice(dice);
        game.setDevelopmentDeck(developmentDeck);
        game.setLongestRoad(longestRoad);
        game.setLargestArmy(largestArmy);
        game.setLatestTradeRequest("{\"type\":\"BANK_TRADE\"}");

        assertSame(currentPlayer, game.getCurrentPlayer());
        assertSame(robber, game.getRobber());
        assertSame(dice, game.getDice());
        assertSame(developmentDeck, game.getDevelopmentDeck());
        assertSame(longestRoad, game.getLongestRoad());
        assertSame(largestArmy, game.getLargestArmy());
        assertEquals("{\"type\":\"BANK_TRADE\"}", game.getLatestTradeRequest());
    }

    private Player player(Long id) {
        Player player = new Player();
        player.setId(id);
        return player;
    }
}
