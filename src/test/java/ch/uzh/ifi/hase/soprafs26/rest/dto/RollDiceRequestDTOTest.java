package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class RollDiceRequestDTOTest {

    @Test
    void gettersAndSetters_storeValues() {
        RollDiceRequestDTO dto = new RollDiceRequestDTO();
        Map<String, Integer> discardResources = Map.of("wood", 1);

        dto.setExpectedGameVersion(3L);
        dto.setDiscardResources(discardResources);

        assertEquals(3L, dto.getExpectedGameVersion());
        assertEquals(discardResources, dto.getDiscardResources());
    }
}
