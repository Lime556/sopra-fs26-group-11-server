package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameChatMessageDTOTest {

    @Test
    void testGettersAndSetters() {
        // create instance
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        
        // define test values
        Long playerId = 1L;
        String playerName = "Alice";
        String text = "Hello everyone!";
        
        // set values
        gameChatMessageDTO.setPlayerId(playerId);
        gameChatMessageDTO.setPlayerName(playerName);
        gameChatMessageDTO.setText(text);
        
        // verify values
        assertEquals(playerId, gameChatMessageDTO.getPlayerId());
        assertEquals(playerName, gameChatMessageDTO.getPlayerName());
        assertEquals(text, gameChatMessageDTO.getText());
    }

    @Test
    void testPlayerIdField() {
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        Long playerId = 42L;
        gameChatMessageDTO.setPlayerId(playerId);
        assertEquals(playerId, gameChatMessageDTO.getPlayerId());
    }

    @Test
    void testPlayerNameField() {
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        String playerName = "Bob";
        gameChatMessageDTO.setPlayerName(playerName);
        assertEquals(playerName, gameChatMessageDTO.getPlayerName());
    }

    @Test
    void testTextField() {
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        String text = "Great move!";
        gameChatMessageDTO.setText(text);
        assertEquals(text, gameChatMessageDTO.getText());
    }

    @Test
    void testInitialValuesAreNull() {
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        assertEquals(null, gameChatMessageDTO.getPlayerId());
        assertEquals(null, gameChatMessageDTO.getPlayerName());
        assertEquals(null, gameChatMessageDTO.getText());
    }

    @Test
    void testMultipleUpdates() {
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        
        // set and verify first value
        gameChatMessageDTO.setPlayerId(1L);
        assertEquals(1L, gameChatMessageDTO.getPlayerId());
        
        // update and verify
        gameChatMessageDTO.setPlayerId(2L);
        assertEquals(2L, gameChatMessageDTO.getPlayerId());
        
        gameChatMessageDTO.setPlayerName("Charlie");
        assertEquals("Charlie", gameChatMessageDTO.getPlayerName());
        gameChatMessageDTO.setPlayerName("Diana");
        assertEquals("Diana", gameChatMessageDTO.getPlayerName());
    }

    @Test
    void testEmptyStrings() {
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        gameChatMessageDTO.setPlayerName("");
        gameChatMessageDTO.setText("");
        
        assertEquals("", gameChatMessageDTO.getPlayerName());
        assertEquals("", gameChatMessageDTO.getText());
    }

    @Test
    void testLongMessageText() {
        GameChatMessageDTO gameChatMessageDTO = new GameChatMessageDTO();
        String longText = "This is a very long message with lots of content and special characters: @#$%^&*()";
        gameChatMessageDTO.setText(longText);
        assertEquals(longText, gameChatMessageDTO.getText());
    }
}
