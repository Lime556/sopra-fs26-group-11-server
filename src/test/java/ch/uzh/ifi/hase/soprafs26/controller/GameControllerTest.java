package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.constant.TimeOfDayMood;
import ch.uzh.ifi.hase.soprafs26.constant.WeatherCategory;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameAmbienceDTO;
import ch.uzh.ifi.hase.soprafs26.service.AmbienceService;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.bot.BotActionExecutionResult;
import ch.uzh.ifi.hase.soprafs26.service.bot.BotActionExecutorService;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private AmbienceService ambienceService;

    @MockitoBean
    @SuppressWarnings("unused")
    private BotActionExecutorService botActionExecutorService;

    @Test
    void endTurn_validRequest_success() throws Exception {
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
    void endTurn_missingToken_returnsUnauthorized() throws Exception {
        given(gameService.endTurn(1L, null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/end-turn");

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endTurn_gameNotFound_returnsNotFound() throws Exception {
        given(gameService.endTurn(999L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id 999 was not found."));

        MockHttpServletRequestBuilder postRequest = post("/games/999/actions/end-turn")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void endTurn_noCurrentPlayer_returnsConflict() throws Exception {
        given(gameService.endTurn(1L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No current player found."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/end-turn")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void getGameState_validRequest_success() throws Exception {
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
    void getGameState_missingToken_returnsUnauthorized() throws Exception {
        given(gameService.getGameById(1L, null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token."));

        MockHttpServletRequestBuilder getRequest = get("/games/1/state");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGameState_gameNotFound_returnsNotFound() throws Exception {
        given(gameService.getGameById(999L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id 999 was not found."));

        MockHttpServletRequestBuilder getRequest = get("/games/999/state")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void getGameState_gameFinished_returnsCorrectStatus() throws Exception {
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
    void getGameAmbience_validTokenAndExistingGame_returnsDto() throws Exception {
        Game game = new Game();
        game.setId(1L);
        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(ambienceService.getCurrentAmbience()).willReturn(new GameAmbienceDTO(
            WeatherCategory.SUNNY,
            TimeOfDayMood.SUNSET,
            "Sunny sunset over the island"
        ));

        MockHttpServletRequestBuilder getRequest = get("/games/1/ambience")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weather", is("SUNNY")))
                .andExpect(jsonPath("$.timeOfDay", is("SUNSET")))
                .andExpect(jsonPath("$.description", is("Sunny sunset over the island")));
    }

    @Test
    void getGameAmbience_missingToken_returnsUnauthorized() throws Exception {
        given(gameService.getGameById(1L, null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token."));

        MockHttpServletRequestBuilder getRequest = get("/games/1/ambience");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGameAmbience_missingGame_returnsNotFound() throws Exception {
        given(gameService.getGameById(999L, "token-123"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id 999 was not found."));

        MockHttpServletRequestBuilder getRequest = get("/games/999/ambience")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void heartbeatGame_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.heartbeatGame(1L, "valid-token")).willReturn(game);

        MockHttpServletRequestBuilder postRequest = post("/games/1/heartbeat")
                .header("Authorization", "valid-token");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

        @Test
        void getDiceRoll_validRequest_success() throws Exception {
                Game game = new Game();
                game.setId(1L);
                game.setDiceValue(11);
                game.setDiceRolledAt(java.time.Instant.parse("2026-04-21T10:15:30Z"));

                    given(gameService.getGameById(1L, "token-123")).willReturn(game);

                    MockHttpServletRequestBuilder getRequest = get("/games/1/dice")
                                    .header("Authorization", "token-123");

                mockMvc.perform(getRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.diceValue", is(11)))
                                .andExpect(jsonPath("$.diceRolledAt", is("2026-04-21T10:15:30Z")));
        }

    @Test
    void rollDice_validRequest_successAndBroadcastsState() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentTurnIndex(0);
        game.setTurnPhase(TurnPhase.ACTION.toString());
        game.setDiceValue(8);

        Player currentPlayer = new Player();
        currentPlayer.setId(10L);
        currentPlayer.setName("ActivePlayer");
        game.setPlayers(List.of(currentPlayer));

        given(gameService.rollDice(1L, "token-123", null)).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/roll-dice")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTurnIndex", is(0)))
                .andExpect(jsonPath("$.turnPhase", is("ACTION")))
                .andExpect(jsonPath("$.diceValue", is(8)))
                .andExpect(jsonPath("$.players[0].id", is(10)))
                .andExpect(jsonPath("$.players[0].name", is("ActivePlayer")));

    }

    @Test
    void rollDice_notInRollPhase_returnsConflictAndDoesNotBroadcast() throws Exception {
        given(gameService.rollDice(1L, "token-123", null))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot roll dice."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/roll-dice")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());

    }

    @Test
    void rollDice_notActivePlayer_returnsForbiddenAndDoesNotBroadcast() throws Exception {
        given(gameService.rollDice(1L, "token-123", null))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the active player can roll dice."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/roll-dice")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());

    }

    @Test
    void publishGameEvent_roadBuilt_returnsBadRequest() throws Exception {
        given(gameService.getGameById(1L, "token-123")).willReturn(new Game());
    
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
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void publishGameEvent_settlementBuilt_returnsBadRequest() throws Exception {
        given(gameService.getGameById(1L, "token-123")).willReturn(new Game());
    
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
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void publishGameEvent_cityBuilt_returnsBadRequest() throws Exception {
        given(gameService.getGameById(1L, "token-123")).willReturn(new Game());
    
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGameState_setupPhase_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentTurnIndex(0);
        game.setGamePhase("SETUP");

        Player currentPlayer = new Player();
        currentPlayer.setId(10L);
        currentPlayer.setName("Player1");

        game.setPlayers(List.of(currentPlayer));

        given(gameService.getGameById(1L, "valid-token")).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);

        MockHttpServletRequestBuilder getRequest = get("/games/1/state")
                .header("Authorization", "valid-token");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(1)))
                .andExpect(jsonPath("$.currentTurnIndex", is(0)))
                .andExpect(jsonPath("$.currentPlayerId", is(10)))
                .andExpect(jsonPath("$.currentPlayerName", is("Player1")))
                .andExpect(jsonPath("$.gameFinished", is(false)));
    }

    @Test
    void buildSettlement_setupPhase_validPlacement_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGamePhase("SETUP");
        game.setCurrentTurnIndex(0);

        Player player1 = new Player();
        player1.setId(10L);
        player1.setName("Player1");

        game.setPlayers(List.of(player1));

        given(gameService.getGameById(1L, "valid-token")).willReturn(game);
        given(gameService.placeInitialSettlement(1L, "valid-token", 10L, 0))
                .willReturn(game);

        String body = """
            {
              "playerId": 10,
              "intersectionId": 0
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-settlement")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void buildSettlement_withExpectedVersionAndSetupPhase_usesVersionedInitialPlacement() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGamePhase("SETUP");

        given(gameService.isSetupPhase(1L, "valid-token")).willReturn(true);
        given(gameService.placeInitialSettlement(1L, "valid-token", 10L, 0, 4L))
                .willReturn(game);

        String body = """
            {
              "playerId": 10,
              "intersectionId": 0,
              "expectedGameVersion": 4
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-settlement")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void buildSettlement_missingPlayerId_returnsBadRequest() throws Exception {
        String body = """
            {
              "intersectionId": 0
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-settlement")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void buildSettlement_invalidPlacement_occupied_badRequest() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGamePhase("SETUP");

        given(gameService.getGameById(1L, "valid-token")).willReturn(game);
        given(gameService.placeInitialSettlement(1L, "valid-token", 10L, 0))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intersection is already occupied."));

        String body = """
            {
              "playerId": 10,
              "intersectionId": 0
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-settlement")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void buildSettlement_notPlayerTurn_conflict() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGamePhase("SETUP");

        given(gameService.getGameById(1L, "valid-token")).willReturn(game);
        given(gameService.placeInitialSettlement(1L, "valid-token", 11L, 0))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "It is not your turn to build."));

        String body = """
            {
              "playerId": 11,
              "intersectionId": 0
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-settlement")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void buildRoad_setupPhase_validPlacement_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGamePhase("SETUP");

        Player player1 = new Player();
        player1.setId(10L);

        game.setPlayers(List.of(player1));

        given(gameService.getGameById(1L, "valid-token")).willReturn(game);
        given(gameService.placeInitialRoad(1L, "valid-token", 10L, 0))
                .willReturn(game);

        String body = """
            {
              "playerId": 10,
              "edgeId": 0
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-road")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void buildRoad_withExpectedVersionAndMainPhase_usesVersionedRoadPurchase() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.isSetupPhase(1L, "valid-token")).willReturn(false);
        given(gameService.addRoadToPlayer(1L, "valid-token", 10L, 0, 4L))
                .willReturn(game);

        String body = """
            {
              "playerId": 10,
              "edgeId": 0,
              "expectedGameVersion": 4
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-road")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void buildRoad_invalidPlacement_occupied_badRequest() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGamePhase("SETUP");

        given(gameService.getGameById(1L, "valid-token")).willReturn(game);
        given(gameService.placeInitialRoad(1L, "valid-token", 10L, 0))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Edge is already occupied."));

        String body = """
            {
              "playerId": 10,
              "edgeId": 0
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-road")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void buildRoad_notPlayerTurn_conflict() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGamePhase("SETUP");

        given(gameService.getGameById(1L, "valid-token")).willReturn(game);
        given(gameService.placeInitialRoad(1L, "valid-token", 11L, 0))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "It is not your turn to build."));

        String body = """
            {
              "playerId": 11,
              "edgeId": 0
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-road")
                .header("Authorization", "valid-token")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void moveRobber_validRequest_successAndBroadcasts() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setRobberTileIndex(8);
        game.setCurrentTurnIndex(0);
        game.setTurnPhase("ACTION");

        Player currentPlayer = new Player();
        currentPlayer.setId(10L);
        currentPlayer.setName("ActivePlayer");
        game.setPlayers(List.of(currentPlayer));

        given(gameService.moveRobber(1L, "token-123", 12)).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/move-robber")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content("12");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTurnIndex", is(0)))
                .andExpect(jsonPath("$.turnPhase", is("ACTION")));

    }

    @Test
    void moveRobber_withSourceAndTarget_successAndBroadcastsFullState() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setRobberTileIndex(12);
        game.setCurrentTurnIndex(0);
        game.setTurnPhase("ACTION");
        game.setRobberMovedAfterSevenRoll(true);

        Player currentPlayer = new Player();
        currentPlayer.setId(10L);
        currentPlayer.setName("ActivePlayer");
        game.setPlayers(List.of(currentPlayer));

        given(gameService.moveRobber(1L, "token-123", 10L, 12, 11L)).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);

        String body = """
            {
              "sourcePlayerId": 10,
              "hexId": 12,
              "targetPlayerId": 11
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/move-robber")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.robberTileIndex", is(12)))
                .andExpect(jsonPath("$.robberMovedAfterSevenRoll", is(true)))
                .andExpect(jsonPath("$.turnPhase", is("ACTION")));

    }

    @Test
    void moveRobber_withExpectedVersion_acceptsPayloadAndMovesRobber() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setRobberTileIndex(12);

        given(gameService.moveRobber(1L, "token-123", 10L, 12, 11L)).willReturn(game);
        given(gameService.moveRobber(1L, "token-123", 10L, 12, 11L, 7L)).willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10,
              "hexId": 12,
              "targetPlayerId": 11,
              "expectedGameVersion": 7
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/move-robber")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.robberTileIndex", is(12)));
    }

    @Test
    void moveRobber_missingHexId_returnsBadRequest() throws Exception {
        String body = """
            {
              "sourcePlayerId": 10
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/move-robber")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void moveRobber_invalidHexId_returnsBadRequest() throws Exception {
        given(gameService.moveRobber(1L, "token-123", 99))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid hex id."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/move-robber")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content("99");

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());

    }

    @Test
    void moveRobber_missingToken_returnsUnauthorized() throws Exception {
        given(gameService.moveRobber(1L, null, 12))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/move-robber")
                .contentType("application/json")
                .content("12");

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());

    }

    @Test
    void moveRobber_sameHexAsCurrentRobber_returnsConflict() throws Exception {
        given(gameService.moveRobber(1L, "token-123", 8))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move robber to the same tile."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/move-robber")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content("8");

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());

    }

    @Test
    void moveRobber_gameNotFound_returnsNotFound() throws Exception {
        given(gameService.moveRobber(999L, "token-123", 12))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with id 999 was not found."));

        MockHttpServletRequestBuilder postRequest = post("/games/999/actions/move-robber")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content("12");

        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void createGame_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGameVersion(1L);

        given(gameService.createGame("token-123", null)).willReturn(game);

        MockHttpServletRequestBuilder postRequest = post("/games")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.gameVersion", is(1)));
    }

    @Test
    void getGameById_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentTurnIndex(0);
                game.setChatMessages(List.of("Player1: Hello", "Player2: Hi"));

        given(gameService.getGameById(1L, "token-123")).willReturn(game);

        MockHttpServletRequestBuilder getRequest = get("/games/1")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.currentTurnIndex", is(0)))
                .andExpect(jsonPath("$.chatMessages[0]", is("Player1: Hello")))
                .andExpect(jsonPath("$.chatMessages[1]", is("Player2: Hi")));
    }

    @Test
    void getGameVersion_validRequest_success() throws Exception {
        ch.uzh.ifi.hase.soprafs26.rest.dto.GameVersionDTO versionDTO = new ch.uzh.ifi.hase.soprafs26.rest.dto.GameVersionDTO();
                versionDTO.setGameId(1L);
        versionDTO.setGameVersion(5L);
                versionDTO.setChatMessageCount(4);
                versionDTO.setEventLogCount(7);

        given(gameService.getGameVersion(1L, "token-123")).willReturn(versionDTO);

        MockHttpServletRequestBuilder getRequest = get("/games/1/version")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(1)))
                .andExpect(jsonPath("$.gameVersion", is(5)))
                .andExpect(jsonPath("$.chatMessageCount", is(4)))
                .andExpect(jsonPath("$.eventLogCount", is(7)));
    }

    @Test
    void updateGame_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.updateGameState(1L, "token-123", null)).willReturn(game);

        MockHttpServletRequestBuilder putRequest = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/games/1")
                .header("Authorization", "token-123");

        mockMvc.perform(putRequest)
                .andExpect(status().isOk());
    }

    @Test
    void publishGameChatMessage_validMessage_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        Player player = new Player();
        player.setId(10L);
        player.setName("Player1");

        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getAuthenticatedPlayer(game, "token-123")).willReturn(player);

        String body = """
            {
              "playerId": 10,
              "playerName": "Player1",
              "text": "Hello everyone!"
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/chat")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.playerId", is(10)))
                .andExpect(jsonPath("$.playerName", is("Player1")))
                .andExpect(jsonPath("$.text", is("Hello everyone!")));

        Mockito.verify(gameService, Mockito.times(1))
                .appendChatMessage(1L, "token-123", "Player1: Hello everyone!");
    }

    @Test
    void publishGameChatMessage_blankName_trimsAndUsesDefaultSender() throws Exception {
        Game game = new Game();
        game.setId(1L);
        Player player = new Player();
        player.setId(10L);
        player.setName("   ");

        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getAuthenticatedPlayer(game, "token-123")).willReturn(player);

        String body = """
            {
              "playerName": "   ",
              "text": "  Hello table  "
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/chat")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted());

        Mockito.verify(gameService).appendChatMessage(1L, "token-123", "Player: Hello table");
    }

    @Test
    void publishGameChatMessage_emptyMessage_isIgnored() throws Exception {
        Game game = new Game();
        game.setId(1L);
        Player player = new Player();
        player.setId(10L);
        player.setName("Player1");

        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getAuthenticatedPlayer(game, "token-123")).willReturn(player);

        String body = """
            {
              "playerId": 10,
              "playerName": "Player1",
              "text": ""
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/chat")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted());

        Mockito.verify(gameService, Mockito.never())
                .appendChatMessage(Mockito.anyLong(), Mockito.any(), Mockito.any());
    }

    @Test
    void publishGameChatMessage_nonParticipant_returnsForbidden() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getAuthenticatedPlayer(game, "token-123")).willReturn(null);

        String body = """
            {
              "playerName": "Intruder",
              "text": "Hello everyone!"
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/chat")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void getBoardById_validRequest_success() throws Exception {
        ch.uzh.ifi.hase.soprafs26.entity.Board board = new ch.uzh.ifi.hase.soprafs26.entity.Board();
        Game game = new Game();
        game.setId(1L);
        game.setBoard(board);

        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getBoardById(1L, "token-123")).willReturn(board);

        MockHttpServletRequestBuilder getRequest = get("/games/1/board")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
    }

    @Test
    void buildCity_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.upgradeSettlementToCity(1L, "token-123", 10L, 5, null))
                .willReturn(game);

        String body = """
            {
              "playerId": 10,
              "intersectionId": 5
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/build-city")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void bankTrade_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.applyBankTrade(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("token-123"), org.mockito.ArgumentMatchers.any()))
                .willReturn(game);

        String body = """
            {
              "type": "BANK_TRADE",
              "sourcePlayerId": 10,
              "giveResource": "WOOD",
              "receiveResource": "BRICK"
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/bank-trade")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void requestPlayerTrade_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.validatePlayerTradeRequest(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("token-123"), org.mockito.ArgumentMatchers.any()))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10,
              "targetPlayerId": 11,
              "giveResource": "WOOD",
              "receiveResource": "BRICK"
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/player-trade/request")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void respondPlayerTrade_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.validatePlayerTradeResponse(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("token-123"), org.mockito.ArgumentMatchers.any()))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10,
              "targetPlayerId": 11,
              "accepted": true
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/player-trade/respond")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void finalizePlayerTrade_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.applyPlayerTrade(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("token-123"), org.mockito.ArgumentMatchers.any()))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10,
              "targetPlayerId": 11
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/player-trade/finalize")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void buyDevelopmentCard_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.buyDevelopmentCard(1L, "token-123", 10L, null))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/development-card/buy")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void playKnightCard_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.playKnightCard(1L, "token-123", 10L, 12, 11L, null))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10,
              "hexId": 12,
              "targetPlayerId": 11
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/development-card/play-knight")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void playRoadBuildingCard_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.playRoadBuildingCard(1L, "token-123", 10L, null))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/development-card/play-road-building")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void playYearOfPlentyCard_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.playYearOfPlentyCard(1L, "token-123", 10L, "WOOD", "BRICK", null))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10,
              "giveResource": "WOOD",
              "secondResource": "BRICK"
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/development-card/play-year-of-plenty")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void playMonopolyCard_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.playMonopolyCard(1L, "token-123", 10L, "WOOD", null))
                .willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10,
              "giveResource": "WOOD"
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/development-card/play-monopoly")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void getGameSync_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setGameVersion(2L);
        game.setCurrentTurnIndex(1);
        game.setTurnPhase(TurnPhase.ACTION.toString());
        game.setGamePhase("MAIN");
        game.setDiceValue(7);
                game.setChatMessages(List.of("Player1: Hello", "Player2: Hi"));
                game.setEventLog(List.of("event1", "event2", "event3"));

        Player currentPlayer = new Player();
        currentPlayer.setId(10L);
        currentPlayer.setName("Player1");

        game.setPlayers(List.of(currentPlayer));

        given(gameService.getGameById(1L, "token-123")).willReturn(game);
        given(gameService.getCurrentPlayer(game)).willReturn(currentPlayer);
        given(gameService.getAuthenticatedPlayer(game, "token-123")).willReturn(currentPlayer);

        MockHttpServletRequestBuilder getRequest = get("/games/1/sync")
                .header("Authorization", "token-123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(1)))
                .andExpect(jsonPath("$.gameVersion", is(2)))
                .andExpect(jsonPath("$.currentTurnIndex", is(1)))
                .andExpect(jsonPath("$.turnPhase", is("ACTION")))
                .andExpect(jsonPath("$.chatMessageCount", is(2)))
                .andExpect(jsonPath("$.eventLogCount", is(3)));
    }

    @Test
    void discardResources_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.discardResources(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("token-123"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(game);

        String body = """
            {
              "wood": 1,
              "brick": 2
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/discard-resources")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void discardResources_withExpectedVersion_removesVersionFromResourceMap() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.discardResources(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.eq("token-123"),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(8L)
        )).willReturn(game);

        String body = """
            {
              "wood": 1,
              "expectedGameVersion": 8
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/discard-resources")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<java.util.Map<String, Integer>> captor =
            org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        Mockito.verify(gameService).discardResources(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.eq("token-123"),
            captor.capture(),
            org.mockito.ArgumentMatchers.eq(8L)
        );
        org.junit.jupiter.api.Assertions.assertEquals(1, captor.getValue().get("wood"));
        org.junit.jupiter.api.Assertions.assertFalse(captor.getValue().containsKey("expectedGameVersion"));
    }

    @Test
    void discardResources_emptyResources_returnsBadRequest() throws Exception {
        String body = """
            {
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/discard-resources")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeBotFallbackAction_validRequest_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(botActionExecutorService.executeBotActionWithResult(1L, "token-123", false))
                .willReturn(new BotActionExecutionResult(game, null, false, false, false, null));

        MockHttpServletRequestBuilder postRequest = post("/games/1/actions/bot/fallback")
                .header("Authorization", "token-123");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    void extractToken_withBearerPrefix_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.getGameById(1L, "my-token-value")).willReturn(game);

        MockHttpServletRequestBuilder getRequest = get("/games/1")
                .header("Authorization", "Bearer my-token-value");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
    }

    @Test
    void extractToken_withoutBearerPrefix_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.getGameById(1L, "plain-token")).willReturn(game);

        MockHttpServletRequestBuilder getRequest = get("/games/1")
                .header("Authorization", "plain-token");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
    }

    @Test
    void publishGameEvent_validNonGameplayEvent_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.getGameById(1L, "token-123")).willReturn(game);

        String body = """
            {
              "type": "CUSTOM_EVENT",
              "sourcePlayerId": 10
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/events")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted());
    }

    @Test
    void publishGameEvent_nullType_success() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(gameService.getGameById(1L, "token-123")).willReturn(game);

        String body = """
            {
              "sourcePlayerId": 10
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/games/1/events")
                .header("Authorization", "token-123")
                .contentType("application/json")
                .content(body);

        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted());
    }
}
