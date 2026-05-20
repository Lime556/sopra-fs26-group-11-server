package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.City;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoatGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DevelopmentDeckGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DiceRollDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameAmbienceDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameSyncDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameVersionDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RollDiceRequestDTO;
import ch.uzh.ifi.hase.soprafs26.service.AmbienceService;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.bot.BotActionExecutionResult;
import ch.uzh.ifi.hase.soprafs26.service.bot.BotActionExecutorService;

@RestController
public class GameController {

    private static final Set<String> GAMEPLAY_EVENT_TYPES = Set.of(
        "ROAD_BUILT",
        "SETTLEMENT_BUILT",
        "CITY_BUILT",
        "BANK_TRADE",
        "PLAYER_TRADE",
        "PLAYER_TRADE_FINALIZE",
        "DEVELOPMENT_CARD_BOUGHT",
        "DEVELOPMENT_CARD_PLAYED_KNIGHT",
        "DEVELOPMENT_CARD_PLAYED_ROAD_BUILDING",
        "DEVELOPMENT_CARD_PLAYED_YEAR_OF_PLENTY",
        "DEVELOPMENT_CARD_PLAYED_MONOPOLY",
        "ROBBER_MOVE",
        "TURN_END"
    );

    private final GameService gameService;
    private final AmbienceService ambienceService;
    private final BotActionExecutorService botActionExecutorService;

    public GameController(
        GameService gameService,
        AmbienceService ambienceService,
        BotActionExecutorService botActionExecutorService
    ) {
        this.gameService = gameService;
        this.ambienceService = ambienceService;
        this.botActionExecutorService = botActionExecutorService;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public GameGetDTO createGame(@RequestBody(required = false) GamePostDTO gamePostDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game createdGame = gameService.createGame(extractToken(authorizationHeader), gamePostDTO);
        return convertGameToDto(createdGame);
    }

    @GetMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO getGameById(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.getGameById(gameId, extractToken(authorizationHeader));
        return convertGameToDto(game);
    }

    @GetMapping("/games/{gameId}/version")
    @ResponseStatus(HttpStatus.OK)
    public GameVersionDTO getGameVersion(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return gameService.getGameVersion(gameId, extractToken(authorizationHeader));
    }

    @GetMapping("/games/{gameId}/ambience")
    @ResponseStatus(HttpStatus.OK)
    public GameAmbienceDTO getGameAmbience(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        gameService.getGameById(gameId, extractToken(authorizationHeader));
        return ambienceService.getCurrentAmbience();
    }

    @PostMapping("/games/{gameId}/heartbeat")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO heartbeatGame(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.heartbeatGame(gameId, extractToken(authorizationHeader));
        return convertGameToDto(game);
    }

    @PutMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO updateGame(@PathVariable Long gameId,
            @RequestBody(required = false) GamePostDTO gamePostDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game updatedGame = gameService.updateGameState(gameId, extractToken(authorizationHeader), gamePostDTO);
        return convertGameToDto(updatedGame);
    }

    @PostMapping("/games/{gameId}/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GameEventDTO publishGameEvent(@PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        gameService.getGameById(gameId, token);
        if (isGameplayEventType(gameEventDTO == null ? null : gameEventDTO.getType())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Gameplay events must use the dedicated action endpoints."
            );
        }
        gameService.appendGameEvent(gameId, token, gameEventDTO);
        return gameEventDTO;
    }

    @PostMapping("/games/{gameId}/chat")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GameChatMessageDTO publishGameChatMessage(@PathVariable Long gameId,
            @RequestBody GameChatMessageDTO gameChatMessageDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        Game game = gameService.getGameById(gameId, token);
        Player authenticatedPlayer = gameService.getAuthenticatedPlayer(game, token);
        if (authenticatedPlayer == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this game.");
        }

        String sender = (authenticatedPlayer.getName() == null || authenticatedPlayer.getName().isBlank())
                ? ((gameChatMessageDTO.getPlayerName() == null || gameChatMessageDTO.getPlayerName().isBlank())
                    ? "Player"
                    : gameChatMessageDTO.getPlayerName().trim())
                : authenticatedPlayer.getName().trim();
        String text = gameChatMessageDTO.getText() == null ? "" : gameChatMessageDTO.getText().trim();
        if (!text.isEmpty()) {
            String formattedMessage = String.format("%s: %s", sender, text);
            gameService.appendChatMessage(gameId, token, formattedMessage);
        }

        gameChatMessageDTO.setPlayerId(authenticatedPlayer.getId());
        gameChatMessageDTO.setPlayerName(sender);
        gameChatMessageDTO.setText(text);

        return gameChatMessageDTO;
    }

    @GetMapping("/games/{gameId}/board")
    @ResponseStatus(HttpStatus.OK)
    public BoardGetDTO getBoardById(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return convertBoardToDto(gameService.getBoardById(gameId, extractToken(authorizationHeader)));
    }

    @GetMapping("/games/{gameId}/state")
    @ResponseStatus(HttpStatus.OK)
    public GameStateDTO getGameState(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.getGameById(gameId, extractToken(authorizationHeader));
        Player currentPlayer = gameService.getCurrentPlayer(game);
        int totalResources = currentPlayer != null ? 
            (Optional.ofNullable(currentPlayer.getWood()).orElse(0) +
             Optional.ofNullable(currentPlayer.getBrick()).orElse(0) +
             Optional.ofNullable(currentPlayer.getWool()).orElse(0) +
             Optional.ofNullable(currentPlayer.getWheat()).orElse(0) +
             Optional.ofNullable(currentPlayer.getOre()).orElse(0)) : 0;
        Boolean currentPlayerMustDiscard = playerMustDiscardForCurrentSevenRoll(game, currentPlayer, totalResources);
        return new GameStateDTO(
            game.getId(),
            game.getCurrentTurnIndex(),
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null,
            currentPlayerMustDiscard
        );
    }

    @PostMapping("/games/{gameId}/actions/roll-dice")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO rollDice(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) RollDiceRequestDTO request) {
        Game game = gameService.rollDice(gameId, extractToken(authorizationHeader), request);
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/move-robber")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO moveRobber(@PathVariable Long gameId,
            @RequestBody Object body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        Game game;
        if (body instanceof Number number) {
            game = gameService.moveRobber(gameId, token, number.intValue());
        } else if (body instanceof Map<?, ?> payload) {
            Integer hexId = readRequiredInteger(payload, "hexId");
            Long sourcePlayerId = readOptionalLong(payload, "sourcePlayerId");
            Long targetPlayerId = readOptionalLong(payload, "targetPlayerId");
            game = sourcePlayerId == null
                ? gameService.moveRobber(gameId, token, hexId)
                : gameService.moveRobber(gameId, token, sourcePlayerId, hexId, targetPlayerId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid robber move payload.");
        }
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/end-turn")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO endTurn(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) Map<String, Object> body) {
        Long expectedGameVersion = readOptionalLong(body, "expectedGameVersion");
        Game game = expectedGameVersion == null
            ? gameService.endTurn(gameId, extractToken(authorizationHeader))
            : gameService.endTurn(gameId, extractToken(authorizationHeader), expectedGameVersion);
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/bot/fallback")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO executeBotFallbackAction(@PathVariable Long gameId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        boolean useAi = readOptionalBoolean(body, "useAi", false);
        BotActionExecutionResult result = botActionExecutorService.executeBotActionWithResult(gameId, token, useAi);
        Game game = result.game();

        if (result.fallbackUsed() || result.aiConsultantUsed()) {
            GameEventDTO event = new GameEventDTO();
            event.setType("ACTION");
            event.setSourcePlayerId(result.playerId());
            event.setBotAiRequested(result.aiRequested());
            event.setBotAiFallbackUsed(result.fallbackUsed());
            event.setBotAiConsultantUsed(result.aiConsultantUsed());
            if (result.aiConsultantUsed()) {
                event.setMessage("Bot AI consultant was used.");
            } else if (result.aiRequested()) {
                event.setMessage("Bot AI skipped/fallback used: " + result.fallbackReason());
            } else {
                event.setMessage("Bot deterministic fallback was used.");
            }
            game = gameService.appendGameEventAndReturnGame(gameId, token, event);
        }

        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/build-settlement")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO buildSettlement(
            @PathVariable Long gameId,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long playerId = readRequiredLong(body, "playerId");
        Integer intersectionId = readRequiredInteger(body, "intersectionId");
        Long expectedGameVersion = readOptionalLong(body, "expectedGameVersion");
        Game updatedGame;

        boolean setupPhase = expectedGameVersion == null
            ? gameService.getGameById(gameId, extractToken(authorizationHeader)).isSetupPhase()
            : gameService.isSetupPhase(gameId, extractToken(authorizationHeader));
        if (setupPhase) {
            updatedGame = expectedGameVersion == null
                ? gameService.placeInitialSettlement(gameId, extractToken(authorizationHeader), playerId, intersectionId)
                : gameService.placeInitialSettlement(gameId, extractToken(authorizationHeader), playerId, intersectionId, expectedGameVersion);
        } else {
            updatedGame = expectedGameVersion == null
                ? gameService.addSettlementToPlayer(gameId, extractToken(authorizationHeader), playerId, intersectionId)
                : gameService.addSettlementToPlayer(gameId, extractToken(authorizationHeader), playerId, intersectionId, expectedGameVersion);
        }
        return convertGameToDto(updatedGame);
    }

    @PostMapping("/games/{gameId}/actions/build-road")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO buildRoad(
            @PathVariable Long gameId,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long playerId = readRequiredLong(body, "playerId");
        Integer edgeId = readRequiredInteger(body, "edgeId");
        Long expectedGameVersion = readOptionalLong(body, "expectedGameVersion");
        Game updatedGame;
        boolean setupPhase = expectedGameVersion == null
            ? gameService.getGameById(gameId, extractToken(authorizationHeader)).isSetupPhase()
            : gameService.isSetupPhase(gameId, extractToken(authorizationHeader));
        if (setupPhase) {
            updatedGame = expectedGameVersion == null
                ? gameService.placeInitialRoad(gameId, extractToken(authorizationHeader), playerId, edgeId)
                : gameService.placeInitialRoad(gameId, extractToken(authorizationHeader), playerId, edgeId, expectedGameVersion);
        } else {
            updatedGame = expectedGameVersion == null
                ? gameService.addRoadToPlayer(gameId, extractToken(authorizationHeader), playerId, edgeId)
                : gameService.addRoadToPlayer(gameId, extractToken(authorizationHeader), playerId, edgeId, expectedGameVersion);
        }
        return convertGameToDto(updatedGame);
    }

    @PostMapping("/games/{gameId}/actions/build-city")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO buildCity(
            @PathVariable Long gameId,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.upgradeSettlementToCity(
            gameId,
            extractToken(authorizationHeader),
            readRequiredLong(body, "playerId"),
            readRequiredInteger(body, "intersectionId"),
            readOptionalLong(body, "expectedGameVersion")
        );
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/bank-trade")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO bankTrade(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        gameEventDTO.setType("BANK_TRADE");
        Game game = gameService.applyBankTrade(gameId, extractToken(authorizationHeader), gameEventDTO);
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/player-trade/request")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO requestPlayerTrade(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        gameEventDTO.setType("PLAYER_TRADE");
        gameEventDTO.setTradeAction("REQUEST");
        Game game = gameService.validatePlayerTradeRequest(gameId, extractToken(authorizationHeader), gameEventDTO);
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/player-trade/respond")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO respondPlayerTrade(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        gameEventDTO.setType("PLAYER_TRADE");
        Game game = gameService.validatePlayerTradeResponse(gameId, extractToken(authorizationHeader), gameEventDTO);
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/player-trade/finalize")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO finalizePlayerTrade(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        gameEventDTO.setType("PLAYER_TRADE_FINALIZE");
        Game game = gameService.applyPlayerTrade(gameId, extractToken(authorizationHeader), gameEventDTO);
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/development-card/buy")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO buyDevelopmentCard(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.buyDevelopmentCard(
            gameId,
            extractToken(authorizationHeader),
            gameEventDTO.getSourcePlayerId(),
            gameEventDTO.getExpectedGameVersion()
        );
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/development-card/play-knight")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO playKnightCard(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.playKnightCard(
            gameId,
            extractToken(authorizationHeader),
            gameEventDTO.getSourcePlayerId(),
            gameEventDTO.getHexId(),
            gameEventDTO.getTargetPlayerId(),
            gameEventDTO.getExpectedGameVersion()
        );
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/development-card/play-road-building")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO playRoadBuildingCard(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.playRoadBuildingCard(
            gameId,
            extractToken(authorizationHeader),
            gameEventDTO.getSourcePlayerId(),
            gameEventDTO.getExpectedGameVersion()
        );
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/development-card/play-year-of-plenty")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO playYearOfPlentyCard(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.playYearOfPlentyCard(
            gameId,
            extractToken(authorizationHeader),
            gameEventDTO.getSourcePlayerId(),
            gameEventDTO.getGiveResource(),
            gameEventDTO.getSecondResource(),
            gameEventDTO.getExpectedGameVersion()
        );
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/development-card/play-monopoly")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO playMonopolyCard(
            @PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.playMonopolyCard(
            gameId,
            extractToken(authorizationHeader),
            gameEventDTO.getSourcePlayerId(),
            gameEventDTO.getGiveResource(),
            gameEventDTO.getExpectedGameVersion()
        );
        return convertGameToDto(game);
    }

    private GameGetDTO convertGameToDto(Game game) {
        GameGetDTO dto = new GameGetDTO();
        dto.setId(game.getId());
        dto.setGameVersion(game.getGameVersion());
        dto.setBoard(convertBoardToDto(game.getBoard()));
        dto.setCurrentTurnIndex(game.getCurrentTurnIndex());
        dto.setTurnPhase(game.getTurnPhase());
        dto.setGamePhase(game.getGamePhase());
        dto.setDiceValue(game.getDiceValue());
        dto.setDiceRolledAt(game.getDiceRolledAt() == null ? null : game.getDiceRolledAt().toString());
        dto.setTradeRequestedAt(game.getTradeRequestedAt() == null ? null : game.getTradeRequestedAt().toString());
        dto.setLatestTradeRequest(game.getLatestTradeRequest());
        dto.setRobberTileIndex(game.getRobberTileIndex());
        dto.setTargetVictoryPoints(game.getTargetVictoryPoints());
        dto.setStartedAt(game.getStartedAt());
        dto.setFinishedAt(game.getFinishedAt());
        dto.setDevelopmentDeck(convertDevelopmentDeckToDto(game));
        dto.setPlayers(convertPlayersToDto(game.getPlayers(), game.getBoard()));
        dto.setWinner(convertPlayerToDto(game.getWinner()));
        dto.setGameFinished(game.getFinishedAt() != null && game.getWinner() != null);
        dto.setEventLog(game.getEventLog() == null ? Collections.emptyList() : new ArrayList<>(game.getEventLog()));
        dto.setChatMessages(game.getChatMessages() == null ? Collections.emptyList() : new ArrayList<>(game.getChatMessages()));
        dto.setBankResources(readBankResources(game));
        dto.setRobberMovedAfterSevenRoll(game.getRobberMovedAfterSevenRoll());
        return dto;
    }

    private Map<String, Integer> readBankResources(Game game) {
        if (game == null) {
            return Collections.emptyMap();
        }

        return Map.of(
            "wood", Optional.ofNullable(game.getBankWood()).orElse(0),
            "brick", Optional.ofNullable(game.getBankBrick()).orElse(0),
            "wool", Optional.ofNullable(game.getBankWool()).orElse(0),
            "wheat", Optional.ofNullable(game.getBankWheat()).orElse(0),
            "ore", Optional.ofNullable(game.getBankOre()).orElse(0)
        );
    }

    private List<PlayerGetDTO> convertPlayersToDto(List<Player> players, Board board) {
        if (players == null) {
            return Collections.emptyList();
        }

        return players.stream().map(p -> convertPlayerToDto(p, board)).toList();
    }

    private PlayerGetDTO convertPlayerToDto(Player player) {
        return convertPlayerToDto(player, null);
    }

    private PlayerGetDTO convertPlayerToDto(Player player, Board board) {
        if (player == null) {
            return null;
        }

        PlayerGetDTO dto = new PlayerGetDTO();
        dto.setId(player.getId());
        dto.setUserId(player.getUser() != null ? player.getUser().getId() : null);
        dto.setColor(player.getColor());
        dto.setName(player.getName());
        dto.setBot(player.isBot());
        dto.setOnline(player.getOnline() == null ? Boolean.TRUE : player.getOnline());
        dto.setLastSeenAt(player.getLastSeenAt() == null ? null : player.getLastSeenAt().toString());
        dto.setDisconnectedAt(player.getDisconnectedAt() == null ? null : player.getDisconnectedAt().toString());
        dto.setVictoryPoints(player.getVictoryPoints());
        dto.setSettlementPoints(player.getSettlementPoints());
        dto.setCityPoints(player.getCityPoints());
        dto.setDevelopmentCardVictoryPoints(player.getDevelopmentCardVictoryPoints());
        dto.setHasLongestRoad(player.getHasLongestRoad());
        dto.setHasLargestArmy(player.getHasLargestArmy());
        dto.setWood(player.getWood());
        dto.setBrick(player.getBrick());
        dto.setWool(player.getWool());
        dto.setWheat(player.getWheat());
        dto.setOre(player.getOre());
        dto.setDevelopmentCards(player.getDevelopmentCards());
        dto.setKnightsPlayed(player.getKnightsPlayed());
        dto.setFreeRoadBuildsRemaining(player.getFreeRoadBuildsRemaining());

        if (board != null && player.getId() != null) {
            List<Map<String, Integer>> settlements = new ArrayList<>();
            List<Map<String, Integer>> cities = new ArrayList<>();
            List<Map<String, Integer>> roads = new ArrayList<>();

            for (Intersection inter : board.getIntersections()) {
                if (inter.getBuilding() != null && player.getId().equals(inter.getBuilding().getOwnerPlayerId())) {
                    List<Map<String, Integer>> coords = board.getHexCoordinatesForIntersection(inter.getId());
                    if (!coords.isEmpty()) {
                        if (inter.getBuilding() instanceof Settlement) settlements.add(coords.get(0));
                        else if (inter.getBuilding() instanceof City) cities.add(coords.get(0));
                    }
                }
            }

            board.getEdges().stream()
                .filter(e -> e.getRoad() != null && player.getId().equals(e.getRoad().getOwnerPlayerId()))
                .forEach(e -> {
                    List<Map<String, Integer>> coords = board.getHexCoordinatesForEdge(e.getId());
                    if (!coords.isEmpty()) {
                        roads.add(coords.get(0));
                    }
                });

            dto.setSettlementsOnCorners(settlements);
            dto.setCitiesOnCorners(cities);
            dto.setRoadsOnEdges(roads);
        }

        return dto;
    }

    private DevelopmentDeckGetDTO convertDevelopmentDeckToDto(Game game) {
        DevelopmentDeckGetDTO dto = new DevelopmentDeckGetDTO();
        dto.setKnight(game.getDevelopmentKnightRemaining());
        dto.setVictoryPoint(game.getDevelopmentVictoryPointRemaining());
        dto.setRoadBuilding(game.getDevelopmentRoadBuildingRemaining());
        dto.setYearOfPlenty(game.getDevelopmentYearOfPlentyRemaining());
        dto.setMonopoly(game.getDevelopmentMonopolyRemaining());

        int remaining = Optional.ofNullable(game.getDevelopmentKnightRemaining()).orElse(0)
            + Optional.ofNullable(game.getDevelopmentVictoryPointRemaining()).orElse(0)
            + Optional.ofNullable(game.getDevelopmentRoadBuildingRemaining()).orElse(0)
            + Optional.ofNullable(game.getDevelopmentYearOfPlentyRemaining()).orElse(0)
            + Optional.ofNullable(game.getDevelopmentMonopolyRemaining()).orElse(0);
        dto.setRemaining(remaining);

        return dto;
    }

    private BoardGetDTO convertBoardToDto(Board board) {
        if (board == null) {
            return null;
        }

        BoardGetDTO dto = new BoardGetDTO();
        dto.setHexTiles(board.getHexTiles());
        dto.setIntersections(board.getIntersections());
        dto.setEdges(board.getEdges());
        dto.setPorts(board.getPorts());
        dto.setBoats(convertBoatsToDto(board.getBoats()));
        dto.setHexTile_DiceNumbers(board.getHexTile_DiceNumbers());
        return dto;
    }

    private List<BoatGetDTO> convertBoatsToDto(List<Boat> boats) {
        if (boats == null) {
            return Collections.emptyList();
        }

        return boats.stream().map(this::convertBoatToDto).toList();
    }

    private BoatGetDTO convertBoatToDto(Boat boat) {
        BoatGetDTO dto = new BoatGetDTO();
        dto.setId(boat.getId());
        dto.setBoatType(boat.getBoatType());
        dto.setHexId(boat.getHexId());
        dto.setFirstCorner(boat.getFirstCorner());
        dto.setSecondCorner(boat.getSecondCorner());
        return dto;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length()).trim();
        }
        return authorizationHeader.trim();
    }

    private static boolean isGameplayEventType(String type) {
        return type != null && GAMEPLAY_EVENT_TYPES.contains(type.trim().toUpperCase(Locale.ROOT));
    }

    @GetMapping("/games/{gameId}/dice")
    @ResponseStatus(HttpStatus.OK)
    public DiceRollDTO getDiceRoll(
            @PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.getGameById(gameId, extractToken(authorizationHeader));
        DiceRollDTO dto = new DiceRollDTO();
        dto.setDiceValue(game.getDiceValue());
        dto.setDiceRolledAt(game.getDiceRolledAt());
        return dto;
    }
    private static Long readRequiredLong(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (!(value instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid field: " + key);
        }
        return number.longValue();
    }

    private static Integer readRequiredInteger(Map<?, ?> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (!(value instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid field: " + key);
        }
        return number.intValue();
    }

    private static Long readOptionalLong(Map<?, ?> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid field: " + key);
        }
        return number.longValue();
    }

    private static boolean readOptionalBoolean(Map<?, ?> body, String key, boolean defaultValue) {
        Object value = body == null ? null : body.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Boolean bool)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid field: " + key);
        }
        return bool;
    }

    @GetMapping("/games/{gameId}/sync")
    @ResponseStatus(HttpStatus.OK)
    public GameSyncDTO getGameSync(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        Game game = gameService.getGameById(gameId, token);

        Player currentPlayer = gameService.getCurrentPlayer(game);
        Player requestingPlayer = gameService.getAuthenticatedPlayer(game, token);

        int requestingPlayerTotalResources = requestingPlayer != null
            ? (Optional.ofNullable(requestingPlayer.getWood()).orElse(0)
                + Optional.ofNullable(requestingPlayer.getBrick()).orElse(0)
                + Optional.ofNullable(requestingPlayer.getWool()).orElse(0)
                + Optional.ofNullable(requestingPlayer.getWheat()).orElse(0)
                + Optional.ofNullable(requestingPlayer.getOre()).orElse(0))
            : 0;

        Boolean currentPlayerMustDiscard = playerMustDiscardForCurrentSevenRoll(game, requestingPlayer, requestingPlayerTotalResources);

        GameSyncDTO dto = new GameSyncDTO();
        dto.setGameId(game.getId());
        dto.setGameVersion(game.getGameVersion());
        dto.setCurrentTurnIndex(game.getCurrentTurnIndex());
        dto.setTurnPhase(game.getTurnPhase());
        dto.setGamePhase(game.getGamePhase());
        dto.setDiceValue(game.getDiceValue());
        dto.setDiceRolledAt(game.getDiceRolledAt() == null ? null : game.getDiceRolledAt().toString());
        dto.setTradeRequestedAt(game.getTradeRequestedAt() == null ? null : game.getTradeRequestedAt().toString());
        dto.setLatestTradeRequest(game.getLatestTradeRequest());
        dto.setChatMessageCount(game.getChatMessages() == null ? 0 : game.getChatMessages().size());
        dto.setEventLogCount(game.getEventLog() == null ? 0 : game.getEventLog().size());
        dto.setCurrentPlayerId(currentPlayer != null ? currentPlayer.getId() : null);
        dto.setCurrentPlayerName(currentPlayer != null ? currentPlayer.getName() : null);
        dto.setGameFinished(game.getFinishedAt() != null && game.getWinner() != null);
        dto.setCurrentPlayerMustDiscard(currentPlayerMustDiscard);
        dto.setRobberMovedAfterSevenRoll(game.getRobberMovedAfterSevenRoll());

        return dto;
    }

    private boolean playerMustDiscardForCurrentSevenRoll(Game game, Player player, int totalResources) {
        if (game == null || player == null || player.getId() == null) {
            return false;
        }

        return TurnPhase.DISCARD.toString().equals(game.getTurnPhase())
            && totalResources > 7
            && !Optional.ofNullable(game.getSevenRollDiscardedPlayerIds())
                .orElse(Collections.emptyList())
                .contains(player.getId().toString());
    }

    @PostMapping("/games/{gameId}/actions/discard-resources")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO discardResources(
            @PathVariable Long gameId,
            @RequestBody Map<String, Integer> discardResources,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long expectedGameVersion = discardResources == null || discardResources.get("expectedGameVersion") == null
            ? null
            : discardResources.get("expectedGameVersion").longValue();
        Map<String, Integer> sanitizedDiscardResources = discardResources == null
            ? Collections.emptyMap()
            : new java.util.HashMap<>(discardResources);
        sanitizedDiscardResources.remove("expectedGameVersion");

        if (sanitizedDiscardResources.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Discard request must include at least one resource to discard."
            );
        }

        Game game = gameService.discardResources(
            gameId,
            extractToken(authorizationHeader),
            sanitizedDiscardResources,
            expectedGameVersion
        );

        return convertGameToDto(game);
    }


}
