package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameSyncDTOTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        
        // define test values
        Long gameId = 1L;
        Long gameVersion = 100L;
        Integer currentTurnIndex = 2;
        String turnPhase = "PLAYING";
        String gamePhase = "MAIN";
        Integer diceValue = 7;
        String diceRolledAt = "2023-01-01T12:00:00Z";
        String tradeRequestedAt = "2023-01-01T12:01:00Z";
        String latestTradeRequest = "Trade request data";
        Integer chatMessageCount = 5;
        Long currentPlayerId = 10L;
        String currentPlayerName = "Alice";
        Boolean gameFinished = false;
        Boolean currentPlayerMustDiscard = false;
        Boolean robberMovedAfterSevenRoll = false;
        
        // set all values
        gameSyncDTO.setGameId(gameId);
        gameSyncDTO.setGameVersion(gameVersion);
        gameSyncDTO.setCurrentTurnIndex(currentTurnIndex);
        gameSyncDTO.setTurnPhase(turnPhase);
        gameSyncDTO.setGamePhase(gamePhase);
        gameSyncDTO.setDiceValue(diceValue);
        gameSyncDTO.setDiceRolledAt(diceRolledAt);
        gameSyncDTO.setTradeRequestedAt(tradeRequestedAt);
        gameSyncDTO.setLatestTradeRequest(latestTradeRequest);
        gameSyncDTO.setChatMessageCount(chatMessageCount);
        gameSyncDTO.setCurrentPlayerId(currentPlayerId);
        gameSyncDTO.setCurrentPlayerName(currentPlayerName);
        gameSyncDTO.setGameFinished(gameFinished);
        gameSyncDTO.setCurrentPlayerMustDiscard(currentPlayerMustDiscard);
        gameSyncDTO.setRobberMovedAfterSevenRoll(robberMovedAfterSevenRoll);
        
        // verify all values
        assertEquals(gameId, gameSyncDTO.getGameId());
        assertEquals(gameVersion, gameSyncDTO.getGameVersion());
        assertEquals(currentTurnIndex, gameSyncDTO.getCurrentTurnIndex());
        assertEquals(turnPhase, gameSyncDTO.getTurnPhase());
        assertEquals(gamePhase, gameSyncDTO.getGamePhase());
        assertEquals(diceValue, gameSyncDTO.getDiceValue());
        assertEquals(diceRolledAt, gameSyncDTO.getDiceRolledAt());
        assertEquals(tradeRequestedAt, gameSyncDTO.getTradeRequestedAt());
        assertEquals(latestTradeRequest, gameSyncDTO.getLatestTradeRequest());
        assertEquals(chatMessageCount, gameSyncDTO.getChatMessageCount());
        assertEquals(currentPlayerId, gameSyncDTO.getCurrentPlayerId());
        assertEquals(currentPlayerName, gameSyncDTO.getCurrentPlayerName());
        assertEquals(gameFinished, gameSyncDTO.getGameFinished());
        assertEquals(currentPlayerMustDiscard, gameSyncDTO.getCurrentPlayerMustDiscard());
        assertEquals(robberMovedAfterSevenRoll, gameSyncDTO.getRobberMovedAfterSevenRoll());
    }

    @Test
    void testGameIdField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Long gameId = 42L;
        gameSyncDTO.setGameId(gameId);
        assertEquals(gameId, gameSyncDTO.getGameId());
    }

    @Test
    void testGameVersionField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Long gameVersion = 50L;
        gameSyncDTO.setGameVersion(gameVersion);
        assertEquals(gameVersion, gameSyncDTO.getGameVersion());
    }

    @Test
    void testCurrentTurnIndexField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Integer currentTurnIndex = 3;
        gameSyncDTO.setCurrentTurnIndex(currentTurnIndex);
        assertEquals(currentTurnIndex, gameSyncDTO.getCurrentTurnIndex());
    }

    @Test
    void testTurnPhaseField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        String turnPhase = "INITIALPLACEMENT";
        gameSyncDTO.setTurnPhase(turnPhase);
        assertEquals(turnPhase, gameSyncDTO.getTurnPhase());
    }

    @Test
    void testGamePhaseField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        String gamePhase = "ROBBER_AFTER_SEVEN";
        gameSyncDTO.setGamePhase(gamePhase);
        assertEquals(gamePhase, gameSyncDTO.getGamePhase());
    }

    @Test
    void testDiceValueField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Integer diceValue = 12;
        gameSyncDTO.setDiceValue(diceValue);
        assertEquals(diceValue, gameSyncDTO.getDiceValue());
    }

    @Test
    void testDiceRolledAtField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        String diceRolledAt = "2023-01-02T15:30:00Z";
        gameSyncDTO.setDiceRolledAt(diceRolledAt);
        assertEquals(diceRolledAt, gameSyncDTO.getDiceRolledAt());
    }

    @Test
    void testTradeRequestedAtField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        String tradeRequestedAt = "2023-01-02T16:00:00Z";
        gameSyncDTO.setTradeRequestedAt(tradeRequestedAt);
        assertEquals(tradeRequestedAt, gameSyncDTO.getTradeRequestedAt());
    }

    @Test
    void testLatestTradeRequestField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        String latestTradeRequest = "New trade offer";
        gameSyncDTO.setLatestTradeRequest(latestTradeRequest);
        assertEquals(latestTradeRequest, gameSyncDTO.getLatestTradeRequest());
    }

    @Test
    void testChatMessageCountField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Integer chatMessageCount = 15;
        gameSyncDTO.setChatMessageCount(chatMessageCount);
        assertEquals(chatMessageCount, gameSyncDTO.getChatMessageCount());
    }

    @Test
    void testCurrentPlayerIdField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Long currentPlayerId = 20L;
        gameSyncDTO.setCurrentPlayerId(currentPlayerId);
        assertEquals(currentPlayerId, gameSyncDTO.getCurrentPlayerId());
    }

    @Test
    void testCurrentPlayerNameField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        String currentPlayerName = "Bob";
        gameSyncDTO.setCurrentPlayerName(currentPlayerName);
        assertEquals(currentPlayerName, gameSyncDTO.getCurrentPlayerName());
    }

    @Test
    void testGameFinishedField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Boolean gameFinished = true;
        gameSyncDTO.setGameFinished(gameFinished);
        assertEquals(gameFinished, gameSyncDTO.getGameFinished());
    }

    @Test
    void testCurrentPlayerMustDiscardField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Boolean currentPlayerMustDiscard = true;
        gameSyncDTO.setCurrentPlayerMustDiscard(currentPlayerMustDiscard);
        assertEquals(currentPlayerMustDiscard, gameSyncDTO.getCurrentPlayerMustDiscard());
    }

    @Test
    void testRobberMovedAfterSevenRollField() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        Boolean robberMovedAfterSevenRoll = true;
        gameSyncDTO.setRobberMovedAfterSevenRoll(robberMovedAfterSevenRoll);
        assertEquals(robberMovedAfterSevenRoll, gameSyncDTO.getRobberMovedAfterSevenRoll());
    }

    @Test
    void testInitialValuesAreNull() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        assertEquals(null, gameSyncDTO.getGameId());
        assertEquals(null, gameSyncDTO.getGameVersion());
        assertEquals(null, gameSyncDTO.getCurrentTurnIndex());
        assertEquals(null, gameSyncDTO.getTurnPhase());
        assertEquals(null, gameSyncDTO.getGamePhase());
        assertEquals(null, gameSyncDTO.getDiceValue());
        assertEquals(null, gameSyncDTO.getDiceRolledAt());
        assertEquals(null, gameSyncDTO.getTradeRequestedAt());
        assertEquals(null, gameSyncDTO.getLatestTradeRequest());
        assertEquals(null, gameSyncDTO.getChatMessageCount());
        assertEquals(null, gameSyncDTO.getCurrentPlayerId());
        assertEquals(null, gameSyncDTO.getCurrentPlayerName());
        assertEquals(null, gameSyncDTO.getGameFinished());
        assertEquals(null, gameSyncDTO.getCurrentPlayerMustDiscard());
        assertEquals(null, gameSyncDTO.getRobberMovedAfterSevenRoll());
    }

    @Test
    void testMultipleUpdates() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        
        // set and verify first value
        gameSyncDTO.setGameId(1L);
        assertEquals(1L, gameSyncDTO.getGameId());
        
        // update and verify
        gameSyncDTO.setGameId(2L);
        assertEquals(2L, gameSyncDTO.getGameId());
        
        gameSyncDTO.setTurnPhase("TRADING");
        assertEquals("TRADING", gameSyncDTO.getTurnPhase());
        gameSyncDTO.setTurnPhase("BUILDING");
        assertEquals("BUILDING", gameSyncDTO.getTurnPhase());
    }

    @Test
    void testBooleanFields() {
        GameSyncDTO gameSyncDTO = new GameSyncDTO();
        
        gameSyncDTO.setGameFinished(true);
        assertEquals(true, gameSyncDTO.getGameFinished());
        gameSyncDTO.setGameFinished(false);
        assertEquals(false, gameSyncDTO.getGameFinished());
        
        gameSyncDTO.setCurrentPlayerMustDiscard(true);
        assertEquals(true, gameSyncDTO.getCurrentPlayerMustDiscard());
        
        gameSyncDTO.setRobberMovedAfterSevenRoll(true);
        assertEquals(true, gameSyncDTO.getRobberMovedAfterSevenRoll());
    }
}
