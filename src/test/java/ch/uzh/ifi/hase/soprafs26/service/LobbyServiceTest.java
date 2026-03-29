package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LobbyService lobbyService;

    private Lobby lobby;
    private User user;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        lobby = new Lobby();
        lobby.setId(1L);
        lobby.setCapacity(2);
        lobby.setUsers(new HashSet<>());

        user = new User();
        user.setId(10L);
        user.setToken("valid-token");

        Mockito.when(lobbyRepository.save(Mockito.any())).thenReturn(lobby);
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
}
