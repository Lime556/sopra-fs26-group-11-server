package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPostDTO;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    public void getLobbies_returnsLobbies() throws Exception {
        Lobby first = new Lobby();
        first.setId(1L);
        first.setCapacity(4);

        Lobby second = new Lobby();
        second.setId(2L);
        second.setCapacity(3);

        given(lobbyService.getLobbies()).willReturn(List.of(first, second));

        mockMvc.perform(get("/lobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    public void createLobby_validInput_success() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setCapacity(4);
        lobby.setName("Test Lobby");

        User host = new User();
        host.setId(10L);
        host.setUsername("hostUser");

        LobbyParticipant participant = new LobbyParticipant();
        participant.setId(100L);
        participant.setUser(host);
        participant.setBot(false);
        participant.setLobby(lobby);

        lobby.setParticipants(new HashSet<>(List.of(participant)));
        lobby.setHostParticipant(participant);

        LobbyPostDTO lobbyPostDTO = new LobbyPostDTO();
        lobbyPostDTO.setCapacity(4);
        lobbyPostDTO.setName("Test Lobby");

        given(lobbyService.createLobby("token-123", 4, null, "Test Lobby")).willReturn(lobby);

        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .header("Authorization", "token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.capacity", is(4)))
                .andExpect(jsonPath("$.currentParticipants", is(1)))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].id", is(100)))
                .andExpect(jsonPath("$.participants[0].userId", is(10)))
                .andExpect(jsonPath("$.participants[0].username", is("hostUser")))
                .andExpect(jsonPath("$.participants[0].bot", is(false)))
                .andExpect(jsonPath("$.hostParticipantId", is(100)));
    }

    @Test
    public void createLobby_missingToken_returnsUnauthorized() throws Exception {
        LobbyPostDTO lobbyPostDTO = new LobbyPostDTO();
        lobbyPostDTO.setCapacity(4);
        lobbyPostDTO.setName("Test Lobby");

        given(lobbyService.createLobby(null, 4, null, "Test Lobby"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token."));

        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void joinLobby_validInput_success() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setCapacity(2);
        lobby.setName("Test Lobby");

        User user = new User();
        user.setId(10L);
        user.setUsername("testUser");

        LobbyParticipant participant = new LobbyParticipant();
        participant.setId(100L);
        participant.setUser(user);
        participant.setBot(false);
        participant.setLobby(lobby);

        lobby.setParticipants(new HashSet<>(List.of(participant)));
        lobby.setHostParticipant(participant);

        LobbyJoinDTO lobbyJoinDTO = new LobbyJoinDTO();
        lobbyJoinDTO.setLobbyId(1L);

        given(lobbyService.joinLobby(1L, "token-123", null)).willReturn(lobby);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/1/join")
                .header("Authorization", "token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.capacity", is(2)))
                .andExpect(jsonPath("$.currentParticipants", is(1)))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].id", is(100)))
                .andExpect(jsonPath("$.participants[0].userId", is(10)))
                .andExpect(jsonPath("$.participants[0].username", is("testUser")))
                .andExpect(jsonPath("$.participants[0].bot", is(false)))
                .andExpect(jsonPath("$.hostParticipantId", is(100)));
    }

    @Test
    public void joinLobby_wrongPassword_returnsForbidden() throws Exception {
        LobbyJoinDTO lobbyJoinDTO = new LobbyJoinDTO();
        lobbyJoinDTO.setLobbyId(1L);
        lobbyJoinDTO.setPassword("wrong");

        given(lobbyService.joinLobby(1L, "token-123", "wrong"))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong password"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/1/join")
                .header("Authorization", "token-123")
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
                .header("Authorization", "token-123")
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
                .header("Authorization", "token-123")
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
