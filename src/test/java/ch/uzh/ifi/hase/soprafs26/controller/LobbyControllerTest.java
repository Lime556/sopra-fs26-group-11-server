package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LobbyService lobbyService;

    @Test
    public void joinLobby_validInput_success() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setCapacity(2);
        lobby.setUsers(new HashSet<>());

        User user = new User();
        user.setId(10L);
        lobby.getUsers().add(user);

        LobbyJoinDTO lobbyJoinDTO = new LobbyJoinDTO();
        lobbyJoinDTO.setLobbyId(1L);

        given(lobbyService.joinLobby(1L, "token-123", null)).willReturn(lobby);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/1/join")
                .header("Authorization", "Bearer token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.capacity", is(2)))
                .andExpect(jsonPath("$.currentPlayers", is(1)))
                .andExpect(jsonPath("$.playerIds", hasSize(1)))
                .andExpect(jsonPath("$.playerIds[0]", is(10)));
    }

    @Test
    public void joinLobby_wrongPassword_returnsForbidden() throws Exception {
        LobbyJoinDTO lobbyJoinDTO = new LobbyJoinDTO();
        lobbyJoinDTO.setLobbyId(1L);
        lobbyJoinDTO.setPassword("wrong");

        given(lobbyService.joinLobby(1L, "token-123", "wrong"))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong password"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/1/join")
                .header("Authorization", "Bearer token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinDTO));

        mockMvc.perform(postRequest).andExpect(status().isForbidden());
    }

    @Test
    public void joinLobby_fullLobby_returnsConflict() throws Exception {
        LobbyJoinDTO lobbyJoinDTO = new LobbyJoinDTO();
        lobbyJoinDTO.setLobbyId(1L);

        given(lobbyService.joinLobby(1L, "token-123", null))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Lobby full"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/1/join")
                .header("Authorization", "Bearer token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinDTO));

        mockMvc.perform(postRequest).andExpect(status().isConflict());
    }

    @Test
    public void joinLobby_unauthorized_returnsUnauthorized() throws Exception {
        LobbyJoinDTO lobbyJoinDTO = new LobbyJoinDTO();
        lobbyJoinDTO.setLobbyId(1L);

        given(lobbyService.joinLobby(1L, null, null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/1/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinDTO));

        mockMvc.perform(postRequest).andExpect(status().isUnauthorized());
    }

    @Test
    public void joinLobby_notFound_returnsNotFound() throws Exception {
        LobbyJoinDTO lobbyJoinDTO = new LobbyJoinDTO();
        lobbyJoinDTO.setLobbyId(1L);

        given(lobbyService.joinLobby(1L, "token-123", null))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/1/join")
                .header("Authorization", "Bearer token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinDTO));

        mockMvc.perform(postRequest).andExpect(status().isNotFound());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
