package ch.uzh.ifi.hase.soprafs26.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

public class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LobbyService lobbyService;

    private User host;
    private Lobby lobby;
    private User user;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        host = new User();
        host.setId(10L);
        host.setToken("valid-token");
        host.setEmail("host@email.com");

        user = new User();
        user.setId(11L);
        user.setToken("valid-token");
        user.setEmail("user@email.com");

        lobby = new Lobby();
        lobby.setId(1L);
        lobby.setCapacity(2);
        lobby.setUsers(new HashSet<>());

        Mockito.when(lobbyRepository.save(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void createLobby_validInput_success() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(host);

        Lobby createdLobby = lobbyService.createLobby("valid-token", "Test Lobby", 4, null);

        assertEquals("Test Lobby", createdLobby.getName());
        assertEquals(4, createdLobby.getCapacity());
        assertEquals(1, createdLobby.getCurrentPlayers());
    }

    @Test
    public void createLobby_invalidCapacity_throwsBadRequest() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(host);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby("valid-token", "Test Lobby", 7, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void createLobby_missingToken_throwsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby(null, "Test Lobby", 4, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void createLobby_blankName_defaultsToUntitledLobby() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(host);

        Lobby createdLobby = lobbyService.createLobby("valid-token", "   ", 4, null);

        assertEquals("Untitled Lobby", createdLobby.getName());
    }

    @Test
    public void getLobbies_returnsAll() {
        Lobby first = new Lobby();
        first.setId(1L);
        Lobby second = new Lobby();
        second.setId(2L);
        Mockito.when(lobbyRepository.findAll()).thenReturn(List.of(first, second));

        List<Lobby> lobbies = lobbyService.getLobbies();

        assertEquals(2, lobbies.size());
    }

    @Test
    public void joinLobby_validInputs_success() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.joinLobby(1L, "valid-token", null);

        Mockito.verify(lobbyRepository, Mockito.times(1)).save(lobby);
        assertEquals(1, updatedLobby.getCurrentPlayers());
    }

    @Test
    public void joinLobby_missingToken_throwsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, null, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void joinLobby_lobbyNotFound_throwsNotFound() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void joinLobby_wrongPassword_throwsForbidden() {
        lobby.setPassword("secret");

        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", "wrong"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void joinLobby_lobbyFull_throwsConflict() {
        User existingA = new User();
        existingA.setId(101L);
        User existingB = new User();
        existingB.setId(102L);
        lobby.getUsers().add(existingA);
        lobby.getUsers().add(existingB);

        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void joinLobby_userAlreadyInLobby_throwsConflict() {
        lobby.getUsers().add(user);

        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void getLobbyById_validId_success() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        Lobby result = lobbyService.getLobbyById(1L, "valid-token");

        assertEquals(1L, result.getId());
    }

    @Test
    public void getLobbyById_notFound_throwsNotFound() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(1L, "valid-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void getLobbyById_missingToken_throwsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(1L, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
    
    @Test
    public void getLobbyById_invalidToken_throwsUnauthorized() {
        Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(1L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
