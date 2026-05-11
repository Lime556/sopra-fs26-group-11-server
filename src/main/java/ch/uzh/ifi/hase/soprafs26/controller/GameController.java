package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameSyncDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RollDiceRequestDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.bot.BotActionExecutorService;

@RestController
public class GameController {

    private final GameService gameService;
    private final BotActionExecutorService botActionExecutorService;

    public GameController(GameService gameService, BotActionExecutorService botActionExecutorService) {
        this.gameService = gameService;
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
        boolean eventHandled = false;
        if ("ROAD_BUILT".equalsIgnoreCase(gameEventDTO.getType())
            && gameEventDTO.getSourcePlayerId() != null
            && gameEventDTO.getEdge() != null) {
            gameService.addRoadToPlayer(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getEdge()
            );
            eventHandled = true;
        } else if ("SETTLEMENT_BUILT".equalsIgnoreCase(gameEventDTO.getType())
            && gameEventDTO.getSourcePlayerId() != null
            && gameEventDTO.getIntersectionId() != null) {
            gameService.addSettlementToPlayer(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getIntersectionId()
            );
            eventHandled = true;
        } else if ("CITY_BUILT".equalsIgnoreCase(gameEventDTO.getType())
            && gameEventDTO.getSourcePlayerId() != null
            && gameEventDTO.getIntersectionId() != null) {
            gameService.upgradeSettlementToCity(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getIntersectionId()
            );
            eventHandled = true;
        } else if ("BANK_TRADE".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && ((gameEventDTO.getGiveResources() != null && gameEventDTO.getReceiveResources() != null)
                        || (gameEventDTO.getGiveResource() != null
                            && gameEventDTO.getReceiveResource() != null
                            && gameEventDTO.getAmount() != null))) {
            gameService.applyBankTrade(
                gameId,
                token,
                gameEventDTO
            );
            eventHandled = true;
        } else if ("PLAYER_TRADE".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && ((gameEventDTO.getGiveResources() != null && gameEventDTO.getReceiveResources() != null)
                        || (gameEventDTO.getGiveResource() != null
                            && gameEventDTO.getReceiveResource() != null
                            && (gameEventDTO.getGiveAmount() != null || gameEventDTO.getReceiveAmount() != null || gameEventDTO.getAmount() != null)))) {
            if ("REQUEST".equalsIgnoreCase(gameEventDTO.getTradeAction())) {
                gameService.validatePlayerTradeRequest(gameId, token, gameEventDTO);
                eventHandled = true;
            } else if ("ACCEPT".equalsIgnoreCase(gameEventDTO.getTradeAction()) || "DENY".equalsIgnoreCase(gameEventDTO.getTradeAction())) {
                gameService.validatePlayerTradeResponse(gameId, token, gameEventDTO);
                eventHandled = true;
            } else {
                if (gameEventDTO.getTargetPlayerId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
                }
                gameService.applyPlayerTrade(
                    gameId,
                    token,
                    gameEventDTO
                );
                eventHandled = true;
            }
        } else if ("DEVELOPMENT_CARD_BOUGHT".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null) {
            gameService.buyDevelopmentCard(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId()
            );
            eventHandled = true;
        } else if ("DEVELOPMENT_CARD_PLAYED_KNIGHT".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null) {
                    gameService.playKnightCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId(),
                        gameEventDTO.getHexId(),
                        gameEventDTO.getTargetPlayerId()
                    );
                    eventHandled = true;
        } else if ("DEVELOPMENT_CARD_PLAYED_ROAD_BUILDING".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null) {
                    gameService.playRoadBuildingCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId()
            );
            eventHandled = true;
        } else if ("DEVELOPMENT_CARD_PLAYED_YEAR_OF_PLENTY".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && gameEventDTO.getGiveResource() != null
                && gameEventDTO.getSecondResource() != null) {
                    gameService.playYearOfPlentyCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId(),
                        gameEventDTO.getGiveResource(),
                        gameEventDTO.getSecondResource()
                    );
                    eventHandled = true;
        } else if ("DEVELOPMENT_CARD_PLAYED_MONOPOLY".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && gameEventDTO.getGiveResource() != null) {
                    gameService.playMonopolyCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId(),
                        gameEventDTO.getGiveResource()
                    );
                    eventHandled = true;
        } else if ("ROBBER_MOVE".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null) {
                    gameService.moveRobber(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId(),
                        gameEventDTO.getHexId(),
                        gameEventDTO.getTargetPlayerId()
                    );
                    eventHandled = true;
        }
        return gameEventDTO;
    }

    @PostMapping("/games/{gameId}/chat")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GameChatMessageDTO publishGameChatMessage(@PathVariable Long gameId,
            @RequestBody GameChatMessageDTO gameChatMessageDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        gameService.getGameById(gameId, extractToken(authorizationHeader));

        String sender = (gameChatMessageDTO.getPlayerName() == null || gameChatMessageDTO.getPlayerName().isBlank())
                ? "Player"
                : gameChatMessageDTO.getPlayerName().trim();
        String text = gameChatMessageDTO.getText() == null ? "" : gameChatMessageDTO.getText().trim();
        if (!text.isEmpty()) {
            String formattedMessage = String.format("%s: %s", sender, text);
            gameService.appendChatMessage(gameId, extractToken(authorizationHeader), formattedMessage);
        }

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
        Boolean currentPlayerMustDiscard = (game.getDiceValue() != null && game.getDiceValue() == 7 && totalResources > 7);
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
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.endTurn(gameId, extractToken(authorizationHeader));
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/bot/fallback")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO executeBotFallbackAction(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = botActionExecutorService.executeFallbackAction(gameId, extractToken(authorizationHeader));
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
        Game game = gameService.getGameById(gameId, extractToken(authorizationHeader));
        Game updatedGame;

        if (game.isSetupPhase()) {
            updatedGame = gameService.placeInitialSettlement(
                    gameId,
                    extractToken(authorizationHeader),
                    playerId,
                    intersectionId
            );
        } else {
            updatedGame = gameService.addSettlementToPlayer(
                    gameId,
                    extractToken(authorizationHeader),
                    playerId,
                    intersectionId
            );
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
        Game game = gameService.getGameById(gameId, extractToken(authorizationHeader));
        Game updatedGame;
        if (game.isSetupPhase()) {
            updatedGame = gameService.placeInitialRoad(
                    gameId,
                    extractToken(authorizationHeader),
                    playerId,
                    edgeId
            );
        } else {
            updatedGame = gameService.addRoadToPlayer(
                    gameId,
                    extractToken(authorizationHeader),
                    playerId,
                    edgeId
            );
        }
        return convertGameToDto(updatedGame);
    }

    private GameGetDTO convertGameToDto(Game game) {
        GameGetDTO dto = new GameGetDTO();
        dto.setId(game.getId());
        dto.setBoard(convertBoardToDto(game.getBoard()));
        dto.setCurrentTurnIndex(game.getCurrentTurnIndex());
        dto.setTurnPhase(game.getTurnPhase());
        dto.setGamePhase(game.getGamePhase());
        dto.setDiceValue(game.getDiceValue());
        dto.setDiceRolledAt(game.getDiceRolledAt() == null ? null : game.getDiceRolledAt().toString());
        dto.setRobberTileIndex(game.getRobberTileIndex());
        dto.setTargetVictoryPoints(game.getTargetVictoryPoints());
        dto.setStartedAt(game.getStartedAt());
        dto.setFinishedAt(game.getFinishedAt());
        dto.setDevelopmentDeck(convertDevelopmentDeckToDto(game));
        dto.setPlayers(convertPlayersToDto(game.getPlayers(), game.getBoard()));
        dto.setWinner(convertPlayerToDto(game.getWinner()));
        dto.setGameFinished(game.getFinishedAt() != null && game.getWinner() != null);
        dto.setEventLog(game.getEventLog() == null ? Collections.emptyList() : new ArrayList<>(game.getEventLog()));
        dto.setChatMessages(game.getChatMessages());
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

        Boolean currentPlayerMustDiscard = TurnPhase.DISCARD.toString().equals(game.getTurnPhase())
            && requestingPlayer != null
            && requestingPlayerTotalResources > 7;

        GameSyncDTO dto = new GameSyncDTO();
        dto.setGameId(game.getId());
        dto.setGameVersion(game.getGameVersion());
        dto.setCurrentTurnIndex(game.getCurrentTurnIndex());
        dto.setTurnPhase(game.getTurnPhase());
        dto.setGamePhase(game.getGamePhase());
        dto.setDiceValue(game.getDiceValue());
        dto.setDiceRolledAt(game.getDiceRolledAt() == null ? null : game.getDiceRolledAt().toString());
        dto.setCurrentPlayerId(currentPlayer != null ? currentPlayer.getId() : null);
        dto.setCurrentPlayerName(currentPlayer != null ? currentPlayer.getName() : null);
        dto.setGameFinished(game.getFinishedAt() != null && game.getWinner() != null);
        dto.setCurrentPlayerMustDiscard(currentPlayerMustDiscard);
        dto.setRobberMovedAfterSevenRoll(game.getRobberMovedAfterSevenRoll());

        return dto;
    }

    @PostMapping("/games/{gameId}/actions/discard-resources")
    @ResponseStatus(HttpStatus.OK)
    public GameGetDTO discardResources(
            @PathVariable Long gameId,
            @RequestBody Map<String, Integer> discardResources,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        Game game = gameService.discardResources(
            gameId,
            extractToken(authorizationHeader),
            discardResources
        );

        return convertGameToDto(game);
    }


}
