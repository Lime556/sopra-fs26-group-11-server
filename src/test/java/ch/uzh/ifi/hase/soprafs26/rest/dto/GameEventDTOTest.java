package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class GameEventDTOTest {

    @Test
    void gettersAndSetters_storeEventFields() {
        GameEventDTO dto = new GameEventDTO();
        Map<String, Integer> giveResources = Map.of("wood", 1);
        Map<String, Integer> receiveResources = Map.of("brick", 1);

        dto.setType("PLAYER_TRADE");
        dto.setSourcePlayerId(10L);
        dto.setTargetPlayerId(11L);
        dto.setGiveResource("WOOD");
        dto.setReceiveResource("BRICK");
        dto.setAmount(2);
        dto.setGiveAmount(1);
        dto.setReceiveAmount(1);
        dto.setGiveResources(giveResources);
        dto.setReceiveResources(receiveResources);
        dto.setTradeAction("REQUEST");
        dto.setTradeRequestId("trade-1");
        dto.setHexId(4);
        dto.setEdge(5);
        dto.setIntersectionId(6);
        dto.setDevelopmentCard("KNIGHT");
        dto.setSecondResource("ORE");
        dto.setMessage("hello");
        dto.setExpectedGameVersion(7L);

        assertEquals("PLAYER_TRADE", dto.getType());
        assertEquals(10L, dto.getSourcePlayerId());
        assertEquals(11L, dto.getTargetPlayerId());
        assertEquals("WOOD", dto.getGiveResource());
        assertEquals("BRICK", dto.getReceiveResource());
        assertEquals(2, dto.getAmount());
        assertEquals(1, dto.getGiveAmount());
        assertEquals(1, dto.getReceiveAmount());
        assertEquals(giveResources, dto.getGiveResources());
        assertEquals(receiveResources, dto.getReceiveResources());
        assertEquals("REQUEST", dto.getTradeAction());
        assertEquals("trade-1", dto.getTradeRequestId());
        assertEquals(4, dto.getHexId());
        assertEquals(5, dto.getEdge());
        assertEquals(6, dto.getIntersectionId());
        assertEquals("KNIGHT", dto.getDevelopmentCard());
        assertEquals("ORE", dto.getSecondResource());
        assertEquals("hello", dto.getMessage());
        assertEquals(7L, dto.getExpectedGameVersion());
    }
}
