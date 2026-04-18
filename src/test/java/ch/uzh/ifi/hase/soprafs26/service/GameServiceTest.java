package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    public void createGame_recalculatesVictoryPointsAndWinner() {
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setTargetVictoryPoints(10);

        PlayerGetDTO alice = new PlayerGetDTO();
        alice.setId(10L);
        alice.setName("Alice");
        alice.setSettlementPoints(3);
        alice.setCityPoints(4);
        alice.setDevelopmentCardVictoryPoints(1);
        alice.setHasLongestRoad(true);

        PlayerGetDTO bob = new PlayerGetDTO();
        bob.setId(11L);
        bob.setName("Bob");
        bob.setSettlementPoints(2);
        bob.setCityPoints(2);
        bob.setDevelopmentCardVictoryPoints(0);
        bob.setHasLargestArmy(true);

        gamePostDTO.setPlayers(List.of(alice, bob));

        Game createdGame = gameService.createGame("valid-token", gamePostDTO);

        assertNotNull(createdGame.getPlayers());
        assertEquals(2, createdGame.getPlayers().size());
        assertEquals(8, createdGame.getPlayers().get(0).getVictoryPoints());
        assertNull(createdGame.getWinner());
        assertNull(createdGame.getFinishedAt());
    }

    @Test
    public void updateGameState_detectsNewWinnerAndFinishesGame() {
        Game existingGame = new Game();
        existingGame.setId(99L);
        existingGame.setTargetVictoryPoints(10);

        Player alice = new Player();
        alice.setId(10L);
        alice.setName("Alice");
        alice.setSettlementPoints(2);
        alice.setCityPoints(2);
        alice.setDevelopmentCardVictoryPoints(0);

        existingGame.setPlayers(List.of(alice));

        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(existingGame));

        GamePostDTO update = new GamePostDTO();
        PlayerGetDTO updatedAlice = new PlayerGetDTO();
        updatedAlice.setId(10L);
        updatedAlice.setName("Alice");
        updatedAlice.setSettlementPoints(4);
        updatedAlice.setCityPoints(4);
        updatedAlice.setDevelopmentCardVictoryPoints(0);
        updatedAlice.setHasLargestArmy(true);
        update.setPlayers(List.of(updatedAlice));

        Game updatedGame = gameService.updateGameState(99L, "valid-token", update);

        assertEquals(10, updatedGame.getPlayers().get(0).getVictoryPoints());
        assertNotNull(updatedGame.getWinner());
        assertEquals(10L, updatedGame.getWinner().getId());
        assertNotNull(updatedGame.getFinishedAt());
    }

    @Test
    public void updateGameState_withoutPlayerAtTarget_hasNoWinner() {
        Game existingGame = new Game();
        existingGame.setId(100L);
        existingGame.setTargetVictoryPoints(10);

        Mockito.when(gameRepository.findById(100L)).thenReturn(Optional.of(existingGame));

        GamePostDTO update = new GamePostDTO();
        PlayerGetDTO bob = new PlayerGetDTO();
        bob.setId(20L);
        bob.setName("Bob");
        bob.setSettlementPoints(3);
        bob.setCityPoints(2);
        update.setPlayers(List.of(bob));

        Game updatedGame = gameService.updateGameState(100L, "valid-token", update);

        assertEquals(5, updatedGame.getPlayers().get(0).getVictoryPoints());
        assertNull(updatedGame.getWinner());
        assertNull(updatedGame.getFinishedAt());
    }
}