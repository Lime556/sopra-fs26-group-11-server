package ch.uzh.ifi.hase.soprafs26.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class JsonConverterTest {

    private static final String INVALID_SERIALIZED_OBJECT =
        Base64.getEncoder().encodeToString("not a serialized object".getBytes());

    @Test
    void boardConverter_roundTripsBoardAndHandlesBlankInput() {
        BoardJsonConverter converter = new BoardJsonConverter();
        Board board = new Board();
        board.setHexTiles(List.of("WOOD"));

        String serialized = converter.convertToDatabaseColumn(board);
        Board deserialized = converter.convertToEntityAttribute(serialized);

        assertNotNull(serialized);
        assertEquals("WOOD", deserialized.getHexTiles().get(0));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void boardConverter_invalidPayload_throwsIllegalStateException() {
        BoardJsonConverter converter = new BoardJsonConverter();

        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(INVALID_SERIALIZED_OBJECT));
    }

    @Test
    void playerListConverter_roundTripsPlayersAndHandlesBlankInput() {
        PlayerListJsonConverter converter = new PlayerListJsonConverter();
        Player player = new Player();
        player.setId(10L);
        player.setName("Player1");

        String serialized = converter.convertToDatabaseColumn(List.of(player));
        List<Player> deserialized = converter.convertToEntityAttribute(serialized);

        assertNotNull(serialized);
        assertEquals(10L, deserialized.get(0).getId());
        assertEquals("Player1", deserialized.get(0).getName());
        assertNull(converter.convertToDatabaseColumn(null));
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute(" ").isEmpty());
    }

    @Test
    void playerListConverter_invalidPayload_throwsIllegalStateException() {
        PlayerListJsonConverter converter = new PlayerListJsonConverter();

        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(INVALID_SERIALIZED_OBJECT));
    }

    @Test
    void stringListConverter_roundTripsStringsAndHandlesBlankInput() {
        StringListJsonConverter converter = new StringListJsonConverter();

        String serialized = converter.convertToDatabaseColumn(List.of("one", "two"));
        List<String> deserialized = converter.convertToEntityAttribute(serialized);

        assertNotNull(serialized);
        assertEquals(List.of("one", "two"), deserialized);
        assertNull(converter.convertToDatabaseColumn(null));
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
    }

    @Test
    void stringListConverter_invalidPayload_throwsIllegalStateException() {
        StringListJsonConverter converter = new StringListJsonConverter();

        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(INVALID_SERIALIZED_OBJECT));
    }
}
