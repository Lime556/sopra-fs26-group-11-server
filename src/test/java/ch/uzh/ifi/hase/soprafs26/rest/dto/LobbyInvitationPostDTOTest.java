package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LobbyInvitationPostDTOTest {

    @Test
    void receiverId_setAndGetWorks() {
        LobbyInvitationPostDTO dto = new LobbyInvitationPostDTO();
        dto.setReceiverId(42L);

        assertEquals(42L, dto.getReceiverId());
    }

    @Test
    void receiverId_isNullByDefault() {
        LobbyInvitationPostDTO dto = new LobbyInvitationPostDTO();

        assertNull(dto.getReceiverId());
    }
}
