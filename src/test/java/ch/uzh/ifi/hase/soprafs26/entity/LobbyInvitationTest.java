package ch.uzh.ifi.hase.soprafs26.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;

class LobbyInvitationTest {

    @Test
    void gettersAndSetters_workAsExpected() {
        Lobby lobby = new Lobby();
        lobby.setId(7L);

        User sender = new User();
        sender.setId(1L);

        User receiver = new User();
        receiver.setId(2L);

        Instant createdAt = Instant.parse("2026-05-21T10:00:00Z");

        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(100L);
        invitation.setLobby(lobby);
        invitation.setSender(sender);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.PENDING);
        invitation.setCreatedAt(createdAt);

        assertEquals(100L, invitation.getId());
        assertEquals(lobby, invitation.getLobby());
        assertEquals(sender, invitation.getSender());
        assertEquals(receiver, invitation.getReceiver());
        assertEquals(LobbyInvitationStatus.PENDING, invitation.getStatus());
        assertEquals(createdAt, invitation.getCreatedAt());
    }

    @Test
    void fieldsAreNullInitially() {
        LobbyInvitation invitation = new LobbyInvitation();

        assertNull(invitation.getId());
        assertNull(invitation.getLobby());
        assertNull(invitation.getSender());
        assertNull(invitation.getReceiver());
        assertNull(invitation.getStatus());
        assertNull(invitation.getCreatedAt());
    }
}
