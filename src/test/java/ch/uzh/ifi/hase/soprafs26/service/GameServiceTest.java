package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private User user;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setToken("valid-token");
        user.setEmail("user@email.com");

        Mockito.when(gameRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
    }

    @Test
    public void createGame_withoutBoard_generatesBoard() {
        Game createdGame = gameService.createGame("valid-token", null);

        assertNotNull(createdGame.getBoard());
        assertEquals(19, createdGame.getBoard().getHexTiles().size());
    }

    @Test
    public void createGame_missingToken_throwsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.createGame(null, null));

        assertEquals(401, exception.getStatusCode().value());
    }
}