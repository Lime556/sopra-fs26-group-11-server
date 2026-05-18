package ch.uzh.ifi.hase.soprafs26.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BoatTest {

    @Test
    void gettersAndSetters_storeValues() {
        Boat boat = new Boat();
        int[] position = new int[] { 1, 2 };

        boat.setId(5);
        boat.setBoatType("merchant");
        boat.setHexId(4);
        boat.setFirstCorner(0);
        boat.setSecondCorner(1);
        boat.setPosition(position);
        boat.setPlayerId(10);

        assertEquals(5, boat.getId());
        assertEquals("merchant", boat.getBoatType());
        assertEquals(4, boat.getHexId());
        assertEquals(0, boat.getFirstCorner());
        assertEquals(1, boat.getSecondCorner());
        assertArrayEquals(position, boat.getPosition());
        assertEquals(10, boat.getPlayerId());
    }
}
