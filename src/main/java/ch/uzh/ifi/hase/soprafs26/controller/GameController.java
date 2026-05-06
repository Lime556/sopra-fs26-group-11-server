package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
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
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoatGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DevelopmentDeckGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DiceRollDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RollDiceRequestDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.bot.BotActionExecutorService;

@RestController
public class GameController {

    private final GameService gameService;
    private final BotActionExecutorService botActionExecutorService;
    private final SimpMessagingTemplate messaging;

    public GameController(GameService gameService, BotActionExecutorService botActionExecutorService, SimpMessagingTemplate messaging) {
        this.gameService = gameService;
        this.botActionExecutorService = botActionExecutorService;
        this.messaging = messaging;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO createGame(@RequestBody(required = false) GamePostDTO gamePostDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game createdGame = gameService.createGame(extractToken(authorizationHeader), gamePostDTO);
        return convertGameToDto(createdGame);
    }

    @GetMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO getGameById(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.getGameById(gameId, extractToken(authorizationHeader));
        return convertGameToDto(game);
    }

    @PutMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO updateGame(@PathVariable Long gameId,
            @RequestBody(required = false) GamePostDTO gamePostDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game updatedGame = gameService.updateGameState(gameId, extractToken(authorizationHeader), gamePostDTO);
        GameGetDTO dto = convertGameToDto(updatedGame);
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), dto);
        return dto;
    }

    @PostMapping("/games/{gameId}/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
    public GameEventDTO publishGameEvent(@PathVariable Long gameId,
            @RequestBody GameEventDTO gameEventDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        gameService.getGameById(gameId, token);
        Game updatedGame = null;
        if ("ROAD_BUILT".equalsIgnoreCase(gameEventDTO.getType())
            && gameEventDTO.getSourcePlayerId() != null
            && gameEventDTO.getEdge() != null) {
            updatedGame = gameService.addRoadToPlayer(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getEdge()
            );
        } else if ("SETTLEMENT_BUILT".equalsIgnoreCase(gameEventDTO.getType())
            && gameEventDTO.getSourcePlayerId() != null
            && gameEventDTO.getIntersectionId() != null) {
            updatedGame = gameService.addSettlementToPlayer(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getIntersectionId()
            );
        } else if ("CITY_BUILT".equalsIgnoreCase(gameEventDTO.getType())
            && gameEventDTO.getSourcePlayerId() != null
            && gameEventDTO.getIntersectionId() != null) {
            updatedGame = gameService.upgradeSettlementToCity(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getIntersectionId()
            );
        } else if ("BANK_TRADE".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && ((gameEventDTO.getGiveResources() != null && gameEventDTO.getReceiveResources() != null)
                        || (gameEventDTO.getGiveResource() != null
                            && gameEventDTO.getReceiveResource() != null
                            && gameEventDTO.getAmount() != null))) {
            updatedGame = gameService.applyBankTrade(
                gameId,
                token,
                gameEventDTO
            );
        } else if ("PLAYER_TRADE".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && ((gameEventDTO.getGiveResources() != null && gameEventDTO.getReceiveResources() != null)
                        || (gameEventDTO.getGiveResource() != null
                            && gameEventDTO.getReceiveResource() != null
                            && (gameEventDTO.getGiveAmount() != null || gameEventDTO.getReceiveAmount() != null || gameEventDTO.getAmount() != null)))) {
            if ("REQUEST".equalsIgnoreCase(gameEventDTO.getTradeAction())) {
                gameService.validatePlayerTradeRequest(gameId, token, gameEventDTO);
            } else if ("ACCEPT".equalsIgnoreCase(gameEventDTO.getTradeAction()) || "DENY".equalsIgnoreCase(gameEventDTO.getTradeAction())) {
                gameService.validatePlayerTradeResponse(gameId, token, gameEventDTO);
            } else {
                if (gameEventDTO.getTargetPlayerId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
                }
                updatedGame = gameService.applyPlayerTrade(
                    gameId,
                    token,
                    gameEventDTO
                );
            }
        } else if ("DEVELOPMENT_CARD_BOUGHT".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null) {
            updatedGame = gameService.buyDevelopmentCard(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId()
            );
        } else if ("DEVELOPMENT_CARD_PLAYED_KNIGHT".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null) {
                    updatedGame = gameService.playKnightCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId(),
                        gameEventDTO.getHexId(),
                        gameEventDTO.getTargetPlayerId()
                    );
        } else if ("DEVELOPMENT_CARD_PLAYED_ROAD_BUILDING".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null) {
                    updatedGame = gameService.playRoadBuildingCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId()
            );
        } else if ("DEVELOPMENT_CARD_PLAYED_YEAR_OF_PLENTY".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && gameEventDTO.getGiveResource() != null
                && gameEventDTO.getSecondResource() != null) {
                    updatedGame = gameService.playYearOfPlentyCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId(),
                        gameEventDTO.getGiveResource(),
                        gameEventDTO.getSecondResource()
                    );
        } else if ("DEVELOPMENT_CARD_PLAYED_MONOPOLY".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && gameEventDTO.getGiveResource() != null) {
                    updatedGame = gameService.playMonopolyCard(
                        gameId,
                        token,
                        gameEventDTO.getSourcePlayerId(),
                        gameEventDTO.getGiveResource()
                    );
        }
        messaging.convertAndSend(String.format("/topic/games/%d/events", gameId), gameEventDTO);
        if (updatedGame != null) {
            messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), convertGameToDto(updatedGame));
        }
        return gameEventDTO;
    }

    @PostMapping("/games/{gameId}/chat")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
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

        messaging.convertAndSend(String.format("/topic/games/%d/chat", gameId), gameChatMessageDTO);
        return gameChatMessageDTO;
    }

    @GetMapping("/games/{gameId}/board")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BoardGetDTO getBoardById(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return convertBoardToDto(gameService.getBoardById(gameId, extractToken(authorizationHeader)));
    }

    @GetMapping("/games/{gameId}/state")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
    @ResponseBody
    public GameStateDTO rollDice(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) RollDiceRequestDTO request) {
        Game game = gameService.rollDice(gameId, extractToken(authorizationHeader), request);
        Player currentPlayer = gameService.getCurrentPlayer(game);
        int totalResources = currentPlayer != null ? 
            (Optional.ofNullable(currentPlayer.getWood()).orElse(0) +
             Optional.ofNullable(currentPlayer.getBrick()).orElse(0) +
             Optional.ofNullable(currentPlayer.getWool()).orElse(0) +
             Optional.ofNullable(currentPlayer.getWheat()).orElse(0) +
             Optional.ofNullable(currentPlayer.getOre()).orElse(0)) : 0;
        Boolean currentPlayerMustDiscard = (game.getDiceValue() != null && game.getDiceValue() == 7 && totalResources > 7);
        GameStateDTO stateDTO = new GameStateDTO(
            game.getId(),
            game.getCurrentTurnIndex(),
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null,
            currentPlayerMustDiscard
        );
        GameGetDTO fullDTO = convertGameToDto(game);
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), fullDTO);
        return stateDTO;
    }

    @PostMapping("/games/{gameId}/actions/move-robber")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameStateDTO moveRobber(@PathVariable Long gameId,
            @RequestBody Integer hexId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.moveRobber(gameId, extractToken(authorizationHeader), hexId);
        Player currentPlayer = gameService.getCurrentPlayer(game);
        GameStateDTO stateDTO = new GameStateDTO(
            game.getId(),
            game.getCurrentTurnIndex(),
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null,
            false
        );
        GameGetDTO fullDTO = convertGameToDto(game);
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), fullDTO);
        return stateDTO;
    }

    @PostMapping("/games/{gameId}/actions/end-turn")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameStateDTO endTurn(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.endTurn(gameId, extractToken(authorizationHeader));
        Player currentPlayer = gameService.getCurrentPlayer(game);
        GameStateDTO stateDTO = new GameStateDTO(
            game.getId(),
            game.getCurrentTurnIndex(),
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null,
            false
        );
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), stateDTO);
        return stateDTO;
    }

    @PostMapping("/games/{gameId}/actions/bot/fallback")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO executeBotFallbackAction(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = botActionExecutorService.executeFallbackAction(gameId, extractToken(authorizationHeader));
        return convertGameToDto(game);
    }

    @PostMapping("/games/{gameId}/actions/build-settlement")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
        GameGetDTO dto = convertGameToDto(updatedGame);
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), dto);
        return dto;
    }

    @PostMapping("/games/{gameId}/actions/build-road")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
        GameGetDTO dto = convertGameToDto(updatedGame);
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), dto);
        return dto;
    }

    private GameGetDTO convertGameToDto(Game game) {
        GameGetDTO dto = new GameGetDTO();
        dto.setId(game.getId());
        dto.setBoard(convertBoardToDto(game.getBoard()));
        dto.setCurrentTurnIndex(game.getCurrentTurnIndex());
        dto.setTurnPhase(game.getTurnPhase());
        dto.setGamePhase(game.getGamePhase());
        dto.setDiceValue(game.getDiceValue());
        dto.setRobberTileIndex(game.getRobberTileIndex());
        dto.setTargetVictoryPoints(game.getTargetVictoryPoints());
        dto.setStartedAt(game.getStartedAt());
        dto.setFinishedAt(game.getFinishedAt());
        dto.setDevelopmentDeck(convertDevelopmentDeckToDto(game));
        dto.setPlayers(convertPlayersToDto(game.getPlayers(), game.getBoard()));
        dto.setWinner(convertPlayerToDto(game.getWinner()));
        dto.setGameFinished(game.getFinishedAt() != null && game.getWinner() != null);
        dto.setChatMessages(game.getChatMessages());
        dto.setBankResources(readBankResources(game));
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

        return players.stream().map(p -> convertPlayerToDto(p, board)).collect(Collectors.toList());
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
        dto.setName(player.getName());
        dto.setBot(player.isBot());
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

        return boats.stream().map(this::convertBoatToDto).collect(Collectors.toList());
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
    @ResponseBody
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

    private static Integer readRequiredInteger(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (!(value instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid field: " + key);
        }
        return number.intValue();
    }
}
