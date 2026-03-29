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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LobbyService lobbyService;

    private User host;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        host = new User();
        host.setId(10L);
        host.setToken("valid-token");

        Mockito.when(lobbyRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void createLobby_validInput_success() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(host);

        Lobby createdLobby = lobbyService.createLobby("valid-token", 4, null);

        assertEquals(4, createdLobby.getCapacity());
        assertEquals(1, createdLobby.getCurrentPlayers());
    }

    @Test
    public void createLobby_invalidCapacity_throwsBadRequest() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(host);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby("valid-token", 7, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void createLobby_missingToken_throwsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby(null, 4, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
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
}
