package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
                first.setName("First Lobby");
        first.setCapacity(4);

        Lobby second = new Lobby();
        second.setId(2L);
                second.setName("Second Lobby");
        second.setCapacity(3);

        given(lobbyService.getLobbies()).willReturn(List.of(first, second));

        mockMvc.perform(get("/lobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("First Lobby")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Second Lobby")));
    }

    @Test
    public void createLobby_validInput_success() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setId(1L);
                lobby.setName("Test Lobby");
        lobby.setCapacity(4);
        lobby.setUsers(new HashSet<>());

        User host = new User();
        host.setId(10L);
        lobby.getUsers().add(host);

        LobbyPostDTO lobbyPostDTO = new LobbyPostDTO();
        lobbyPostDTO.setName("Test Lobby");
        lobbyPostDTO.setCapacity(4);

        given(lobbyService.createLobby("token-123", "Test Lobby", 4, null)).willReturn(lobby);

        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .header("Authorization", "Bearer token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Lobby")))
                .andExpect(jsonPath("$.capacity", is(4)))
                .andExpect(jsonPath("$.currentPlayers", is(1)));
    }

    @Test
    public void createLobby_missingToken_returnsUnauthorized() throws Exception {
        LobbyPostDTO lobbyPostDTO = new LobbyPostDTO();
        lobbyPostDTO.setName("Test Lobby");
        lobbyPostDTO.setCapacity(4);

        given(lobbyService.createLobby(null, "Test Lobby", 4, null))
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
        lobby.setName("Test Lobby");
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
    
    @Test
    public void getLobbyById_validId_success() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setCapacity(4);
        lobby.setUsers(new HashSet<>());

        User user = new User();
        user.setId(10L);
        lobby.getUsers().add(user);

        given(lobbyService.getLobbyById(1L, "token-123")).willReturn(lobby);

        MockHttpServletRequestBuilder getRequest = get("/lobbies/1")
                .header("Authorization", "Bearer token-123")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Lobby")))
                .andExpect(jsonPath("$.capacity", is(4)))
                .andExpect(jsonPath("$.currentPlayers", is(1)))
                .andExpect(jsonPath("$.playerIds", hasSize(1)))
                .andExpect(jsonPath("$.playerIds[0]", is(10)));
    }

    @Test
    public void getLobbyById_notFound_returnsNotFound() throws Exception {
        given(lobbyService.getLobbyById(1L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder getRequest = get("/lobbies/1")
                .header("Authorization", "Bearer token-123")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getLobbyById_missingToken_returnsUnauthorized() throws Exception {
        given(lobbyService.getLobbyById(1L, null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token"));

        MockHttpServletRequestBuilder getRequest = get("/lobbies/1")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getLobbyById_invalidToken_returnsUnauthorized() throws Exception {
        given(lobbyService.getLobbyById(1L, "invalid-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        MockHttpServletRequestBuilder getRequest = get("/lobbies/1")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
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
