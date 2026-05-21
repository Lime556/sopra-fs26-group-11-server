package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;

class LobbyInvitationGetDTOTest {

    @Test
    void gettersAndSetters_workAsExpected() {
        LobbyInvitationGetDTO dto = new LobbyInvitationGetDTO();
        Instant createdAt = Instant.parse("2026-05-21T10:00:00Z");

        dto.setId(100L);
        dto.setLobbyId(7L);
        dto.setLobbyName("Invite Lobby");
        dto.setSenderId(1L);
        dto.setSenderUsername("sender");
        dto.setReceiverId(2L);
        dto.setReceiverUsername("receiver");
        dto.setStatus(LobbyInvitationStatus.PENDING);
        dto.setCreatedAt(createdAt);

        assertEquals(100L, dto.getId());
        assertEquals(7L, dto.getLobbyId());
        assertEquals("Invite Lobby", dto.getLobbyName());
        assertEquals(1L, dto.getSenderId());
        assertEquals("sender", dto.getSenderUsername());
        assertEquals(2L, dto.getReceiverId());
        assertEquals("receiver", dto.getReceiverUsername());
        assertEquals(LobbyInvitationStatus.PENDING, dto.getStatus());
        assertEquals(createdAt, dto.getCreatedAt());
    }

    @Test
    void initialState_isNullForAllFields() {
        LobbyInvitationGetDTO dto = new LobbyInvitationGetDTO();

        assertNull(dto.getId());
        assertNull(dto.getLobbyId());
        assertNull(dto.getLobbyName());
        assertNull(dto.getSenderId());
        assertNull(dto.getSenderUsername());
        assertNull(dto.getReceiverId());
        assertNull(dto.getReceiverUsername());
        assertNull(dto.getStatus());
        assertNull(dto.getCreatedAt());
    }
}
