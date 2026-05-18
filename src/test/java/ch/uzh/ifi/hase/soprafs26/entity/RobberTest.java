package ch.uzh.ifi.hase.soprafs26.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RobberTest {

    @Test
    void idGetterAndSetter_storeValue() {
        Robber robber = new Robber();

        robber.setId(42L);

        assertEquals(42L, robber.getId());
    }
}
