package ch.uzh.ifi.hase.soprafs26.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyInvitation;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.LobbyInvitationService;

@WebMvcTest(LobbyInvitationController.class)
class LobbyInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LobbyInvitationService lobbyInvitationService;

    private LobbyInvitation buildInvitation(Long id, LobbyInvitationStatus status) {
        User sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");

        User receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");

        Lobby lobby = new Lobby();
        lobby.setId(7L);
        lobby.setName("Test Lobby");

        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(id);
        invitation.setSender(sender);
        invitation.setReceiver(receiver);
        invitation.setLobby(lobby);
        invitation.setStatus(status);
        invitation.setCreatedAt(Instant.parse("2026-05-21T10:00:00Z"));
        return invitation;
    }

    @Test
    void sendLobbyInvitation_validInput_returnsCreated() throws Exception {
        LobbyInvitation invitation = buildInvitation(100L, LobbyInvitationStatus.PENDING);
        given(lobbyInvitationService.sendInvitation("sender-token", 7L, 2L)).willReturn(invitation);

        String body = """
            {
              \"receiverId\": 2
            }
            """;

        MockHttpServletRequestBuilder request = post("/lobbies/7/invites")
            .header("Authorization", "sender-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);

        mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(100)))
            .andExpect(jsonPath("$.lobbyId", is(7)))
            .andExpect(jsonPath("$.senderId", is(1)))
            .andExpect(jsonPath("$.receiverId", is(2)))
            .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void sendLobbyInvitation_missingToken_returnsUnauthorized() throws Exception {
        given(lobbyInvitationService.sendInvitation(null, 7L, 2L))
            .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated"));

        String body = """
            {
              \"receiverId\": 2
            }
            """;

        MockHttpServletRequestBuilder request = post("/lobbies/7/invites")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);

        mockMvc.perform(request)
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getPendingLobbyInvitations_validToken_returnsList() throws Exception {
        LobbyInvitation invitation = buildInvitation(101L, LobbyInvitationStatus.PENDING);
        given(lobbyInvitationService.getPendingInvitations("receiver-token")).willReturn(List.of(invitation));

        MockHttpServletRequestBuilder request = get("/lobby-invitations")
            .header("Authorization", "receiver-token");

        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(101)))
            .andExpect(jsonPath("$[0].senderUsername", is("sender")))
            .andExpect(jsonPath("$[0].receiverUsername", is("receiver")))
            .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    void acceptLobbyInvitation_validInput_returnsAccepted() throws Exception {
        LobbyInvitation invitation = buildInvitation(102L, LobbyInvitationStatus.ACCEPTED);
        given(lobbyInvitationService.acceptInvitation("receiver-token", 102L)).willReturn(invitation);

        MockHttpServletRequestBuilder request = put("/lobby-invitations/102/accept")
            .header("Authorization", "receiver-token");

        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(102)))
            .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }

    @Test
    void acceptLobbyInvitation_notReceiver_returnsForbidden() throws Exception {
        given(lobbyInvitationService.acceptInvitation("sender-token", 102L))
            .willThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the invited user can accept this invitation."
            ));

        MockHttpServletRequestBuilder request = put("/lobby-invitations/102/accept")
            .header("Authorization", "sender-token");

        mockMvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void declineLobbyInvitation_validInput_returnsDeclined() throws Exception {
        LobbyInvitation invitation = buildInvitation(103L, LobbyInvitationStatus.DECLINED);
        given(lobbyInvitationService.declineInvitation("receiver-token", 103L)).willReturn(invitation);

        MockHttpServletRequestBuilder request = put("/lobby-invitations/103/decline")
            .header("Authorization", "receiver-token");

        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(103)))
            .andExpect(jsonPath("$.status", is("DECLINED")));
    }

    @Test
    void declineLobbyInvitation_notFound_returnsNotFound() throws Exception {
        given(lobbyInvitationService.declineInvitation("receiver-token", 999L))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby invitation not found."));

        MockHttpServletRequestBuilder request = put("/lobby-invitations/999/decline")
            .header("Authorization", "receiver-token");

        mockMvc.perform(request)
            .andExpect(status().isNotFound());
    }
}
