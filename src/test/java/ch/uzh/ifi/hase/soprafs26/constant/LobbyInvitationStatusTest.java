package ch.uzh.ifi.hase.soprafs26.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LobbyInvitationStatusTest {

    @Test
    void enumValues_areStable() {
        LobbyInvitationStatus[] values = LobbyInvitationStatus.values();

        assertEquals(3, values.length);
        assertEquals(LobbyInvitationStatus.PENDING, values[0]);
        assertEquals(LobbyInvitationStatus.ACCEPTED, values[1]);
        assertEquals(LobbyInvitationStatus.DECLINED, values[2]);
    }
}
