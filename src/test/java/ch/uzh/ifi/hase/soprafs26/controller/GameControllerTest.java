package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import java.util.List;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    public void endTurn_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentTurnIndex(1);
        game.setTurnPhase(TurnPhase.ROLL_DICE.toString());
        game.setDiceValue(null);

        Player player1 = new Player();
        player1.setId(10L);
        player1.setName("Player1");

        Player player2 = new Player();
        player2.setId(11L);
        player2.setName("Player2");

        game.setPlayers(List.of(player1, player2));

        given(gameService.endTurn(1L, "token-123")).willReturn(game);

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/end-turn")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTurnIndex", is(1)))
                .andExpect(jsonPath("$.turnPhase", is("ROLL_DICE")))
                .andExpect(jsonPath("$.diceValue").doesNotExist());
    }

    @Test
    public void endTurn_missingToken_returnsUnauthorized() throws Exception {
        given(gameService.endTurn(1L, null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/end-turn");

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void endTurn_gameNotFound_returnsNotFound() throws Exception {
        given(gameService.endTurn(999L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id 999 was not found."));

        MockHttpServletRequestBuilder postRequest = post("/games/999/actions/end-turn")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void endTurn_noCurrentPlayer_returnsConflict() throws Exception {
        given(gameService.endTurn(1L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No current player found."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/end-turn")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void getGameState_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentTurnIndex(0);
        game.setTurnPhase(TurnPhase.ACTION.toString());
        game.setDiceValue(7);
        game.setFinishedAt(null);

        Player currentPlayer = new Player();
        currentPlayer.setId(10L);
        currentPlayer.setName("ActivePlayer");

        game.setPlayers(List.of(currentPlayer));

        // Mock the service to return a game state through getCurrentPlayer
        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);

        MockHttpServletRequestBuilder getRequest = get("/games/1/state")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTurnIndex", is(0)))
                .andExpect(jsonPath("$.turnPhase", is("ACTION")))
                .andExpect(jsonPath("$.diceValue", is(7)))
                .andExpect(jsonPath("$.gameFinished", is(false)));
    }

    @Test
    public void getGameState_missingToken_returnsUnauthorized() throws Exception {
        given(gameService.getGameById(1L, null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token."));

        MockHttpServletRequestBuilder getRequest = get("/games/1/state");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGameState_gameNotFound_returnsNotFound() throws Exception {
        given(gameService.getGameById(999L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id 999 was not found."));

        MockHttpServletRequestBuilder getRequest = get("/games/999/state")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getGameState_gameFinished_returnsCorrectStatus() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentTurnIndex(2);
        game.setTurnPhase(TurnPhase.ROLL_DICE.toString());
        game.setDiceValue(null);

        Player currentPlayer = new Player();
        currentPlayer.setId(12L);
        currentPlayer.setName("Winner");

        Player winner = new Player();
        winner.setId(12L);
        winner.setName("Winner");

        game.setPlayers(List.of(currentPlayer));
        game.setWinner(winner);
        game.setFinishedAt(java.time.LocalDateTime.now());

        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);

        MockHttpServletRequestBuilder getRequest = get("/games/1/state")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameFinished", is(true)));
    }

        @Test
        public void getDiceRoll_validRequest_success() throws Exception {
                Game game = new Game();
                game.setId(1L);
                game.setDiceValue(11);
                game.setDiceRolledAt(java.time.Instant.parse("2026-04-21T10:15:30Z"));

                given(gameService.getGameById(1L, null)).willReturn(game);

                MockHttpServletRequestBuilder getRequest = get("/games/1/dice");

                mockMvc.perform(getRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.diceValue", is(11)))
                                .andExpect(jsonPath("$.diceRolledAt", is("2026-04-21T10:15:30Z")));
        }

    @Test
    public void rollDice_validRequest_successAndBroadcastsState() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentTurnIndex(0);
        game.setTurnPhase(TurnPhase.ACTION.toString());
        game.setDiceValue(8);

        Player currentPlayer = new Player();
        currentPlayer.setId(10L);
        currentPlayer.setName("ActivePlayer");
        game.setPlayers(List.of(currentPlayer));

        given(gameService.rollDice(1L, "token-123")).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/roll-dice")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTurnIndex", is(0)))
                .andExpect(jsonPath("$.turnPhase", is("ACTION")))
                .andExpect(jsonPath("$.diceValue", is(8)))
                .andExpect(jsonPath("$.currentPlayerId", is(10)))
                .andExpect(jsonPath("$.currentPlayerName", is("ActivePlayer")));

        verify(messagingTemplate).convertAndSend(eq("/topic/games/1/state"), any(GameStateDTO.class));
    }

    @Test
    public void rollDice_notInRollPhase_returnsConflictAndDoesNotBroadcast() throws Exception {
        given(gameService.rollDice(1L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot roll dice."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/roll-dice")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/games/1/state"), any(GameStateDTO.class));
    }

    @Test
    public void rollDice_notActivePlayer_returnsForbiddenAndDoesNotBroadcast() throws Exception {
        given(gameService.rollDice(1L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the active player can roll dice."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/roll-dice")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/games/1/state"), any(GameStateDTO.class));
    }

    @Test
    public void publishGameEvent_roadBuilt_callsServiceAndReturnsAccepted() throws Exception {
        Game updatedGame = new Game();
        updatedGame.setId(1L);
    
        given(gameService.getGameById(1L, "token-123")).willReturn(new Game());
        given(gameService.addRoadToPlayer(1L, "token-123", 10L, 7)).willReturn(updatedGame);
    
        String body = """
            {
              "type": "ROAD_BUILT",
              "sourcePlayerId": 10,
              "edge": 7
            }
            """;
    
        MockHttpServletRequestBuilder postRequest = post("/games/1/events")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);
    
        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type", is("ROAD_BUILT")))
                .andExpect(jsonPath("$.sourcePlayerId", is(10)))
                .andExpect(jsonPath("$.edge", is(7)));
    }
    
    @Test
    public void publishGameEvent_settlementBuilt_callsServiceAndReturnsAccepted() throws Exception {
        Game updatedGame = new Game();
        updatedGame.setId(1L);
    
        given(gameService.getGameById(1L, "token-123")).willReturn(new Game());
        given(gameService.addSettlementToPlayer(1L, "token-123", 10L, 3)).willReturn(updatedGame);
    
        String body = """
            {
              "type": "SETTLEMENT_BUILT",
              "sourcePlayerId": 10,
              "intersectionId": 3
            }
            """;
    
        MockHttpServletRequestBuilder postRequest = post("/games/1/events")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);
    
        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type", is("SETTLEMENT_BUILT")))
                .andExpect(jsonPath("$.sourcePlayerId", is(10)))
                .andExpect(jsonPath("$.intersectionId", is(3)));
    }
    
    @Test
    public void publishGameEvent_cityBuilt_callsServiceAndReturnsAccepted() throws Exception {
        Game updatedGame = new Game();
        updatedGame.setId(1L);
    
        given(gameService.getGameById(1L, "token-123")).willReturn(new Game());
        given(gameService.upgradeSettlementToCity(1L, "token-123", 10L, 3)).willReturn(updatedGame);
    
        String body = """
            {
              "type": "CITY_BUILT",
              "sourcePlayerId": 10,
              "intersectionId": 3
            }
            """;
    
        MockHttpServletRequestBuilder postRequest = post("/games/1/events")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);
    
        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type", is("CITY_BUILT")))
                .andExpect(jsonPath("$.sourcePlayerId", is(10)))
                .andExpect(jsonPath("$.intersectionId", is(3)));
    }
}