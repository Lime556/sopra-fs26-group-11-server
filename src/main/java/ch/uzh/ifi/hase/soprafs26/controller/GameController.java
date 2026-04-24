package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import ch.uzh.ifi.hase.soprafs26.entity.Building;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.City;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoatGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DevelopmentDeckGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DiceRollDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

@RestController
public class GameController {
    private static final double HEX_SIZE = 58.0;
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double ORIGIN_X = 150.0;
    private static final double ORIGIN_Y = 130.0;
    private static final double HEX_SPACING_X = HEX_SIZE * SQRT_3;
    private static final double HEX_SPACING_Y = HEX_SIZE * 1.5;

    private final GameService gameService;
    private final SimpMessagingTemplate messaging;

    public GameController(GameService gameService, SimpMessagingTemplate messaging) {
        this.gameService = gameService;
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
                && gameEventDTO.getGiveResource() != null
                && gameEventDTO.getReceiveResource() != null
                && gameEventDTO.getAmount() != null) {
            updatedGame = gameService.applyBankTrade(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getGiveResource(),
                gameEventDTO.getReceiveResource(),
                gameEventDTO.getAmount()
            );
        } else if ("PLAYER_TRADE".equalsIgnoreCase(gameEventDTO.getType())
                && gameEventDTO.getSourcePlayerId() != null
                && gameEventDTO.getTargetPlayerId() != null
                && gameEventDTO.getGiveResource() != null
                && gameEventDTO.getReceiveResource() != null
                && gameEventDTO.getAmount() != null) {
            updatedGame = gameService.applyPlayerTrade(
                gameId,
                token,
                gameEventDTO.getSourcePlayerId(),
                gameEventDTO.getTargetPlayerId(),
                gameEventDTO.getGiveResource(),
                gameEventDTO.getReceiveResource(),
                gameEventDTO.getAmount()
            );
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
        return new GameStateDTO(
            game.getId(),
            game.getCurrentTurnIndex(),
            game.getGamePhase(),
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null
        );
    }

    @PostMapping("/games/{gameId}/actions/roll-dice")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameStateDTO rollDice(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Game game = gameService.rollDice(gameId, extractToken(authorizationHeader));
        Player currentPlayer = gameService.getCurrentPlayer(game);
        GameStateDTO stateDTO = new GameStateDTO(
            game.getId(),
            game.getCurrentTurnIndex(),
            game.getGamePhase(),
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null
        );
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), stateDTO);
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
            game.getGamePhase(),
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null
        );
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), stateDTO);
        return stateDTO;
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
        dto.setPlayers(convertPlayersToDto(game));
        dto.setWinner(convertPlayerToDto(game, game.getWinner()));
        dto.setGameFinished(game.getFinishedAt() != null && game.getWinner() != null);
        dto.setChatMessages(game.getChatMessages());
        return dto;
    }

    private List<PlayerGetDTO> convertPlayersToDto(Game game) {
        List<Player> players = game.getPlayers();
        if (players == null) {
            return Collections.emptyList();
        }

        return players.stream().map(player -> convertPlayerToDto(game, player)).collect(Collectors.toList());
    }

    private PlayerGetDTO convertPlayerToDto(Game game, Player player) {
        if (player == null) {
            return null;
        }

        PlayerGetDTO dto = new PlayerGetDTO();
        dto.setId(player.getId());
        dto.setName(player.getName());
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
        dto.setRoadsOnEdges(extractRoadsForPlayer(game, player.getId()));
        dto.setSettlementsOnCorners(extractSettlementsForPlayer(game, player.getId()));
        dto.setCitiesOnCorners(extractCitiesForPlayer(game, player.getId()));
        return dto;
    }

    private List<String> extractRoadsForPlayer(Game game, Long playerId) {
        Board board = game.getBoard();
        if (board == null || board.getEdges() == null || playerId == null) {
            return Collections.emptyList();
        }

        Map<Integer, String> edgeIdToRoadPosition = buildEdgeIdToRoadPositionMap();
        List<String> roads = new ArrayList<>();
        for (Edge edge : board.getEdges()) {
            if (edge == null || edge.getRoad() == null || edge.getRoad().getOwnerPlayerId() == null) {
                continue;
            }
            if (!playerId.equals(edge.getRoad().getOwnerPlayerId())) {
                continue;
            }

            String position = edgeIdToRoadPosition.get(edge.getId());
            if (position != null) {
                roads.add(position);
            }
        }

        return roads;
    }

    private List<Map<String, Integer>> extractSettlementsForPlayer(Game game, Long playerId) {
        return extractBuildingsForPlayer(game, playerId, Settlement.class);
    }

    private List<Map<String, Integer>> extractCitiesForPlayer(Game game, Long playerId) {
        return extractBuildingsForPlayer(game, playerId, City.class);
    }

    private List<Map<String, Integer>> extractBuildingsForPlayer(Game game, Long playerId, Class<? extends Building> expectedType) {
        Board board = game.getBoard();
        if (board == null || board.getIntersections() == null || playerId == null) {
            return Collections.emptyList();
        }

        Map<Integer, Map<String, Integer>> intersectionIdToCornerPosition = buildIntersectionIdToCornerPositionMap();
        List<Map<String, Integer>> positions = new ArrayList<>();

        for (Intersection intersection : board.getIntersections()) {
            if (intersection == null || intersection.getBuilding() == null) {
                continue;
            }

            Building building = intersection.getBuilding();
            if (!expectedType.isInstance(building) || building.getOwnerPlayerId() == null || !playerId.equals(building.getOwnerPlayerId())) {
                continue;
            }

            Map<String, Integer> position = intersectionIdToCornerPosition.get(intersection.getId());
            if (position != null) {
                positions.add(position);
            }
        }

        return positions;
    }

    private Map<Integer, Map<String, Integer>> buildIntersectionIdToCornerPositionMap() {
        Map<String, Integer> cornerKeyToIntersectionId = new LinkedHashMap<>();
        Map<Integer, Map<String, Integer>> intersectionIdToPosition = new HashMap<>();
        int nextIntersectionId = 0;

        for (int hexId = 1; hexId <= 19; hexId++) {
            double[] center = toPixel(hexId);
            for (int corner = 0; corner < 6; corner++) {
                double[] point = getCornerPoint(center[0], center[1], corner);
                String cornerKey = formatPoint(point[0], point[1]);

                Integer intersectionId = cornerKeyToIntersectionId.get(cornerKey);
                if (intersectionId == null) {
                    intersectionId = nextIntersectionId++;
                    cornerKeyToIntersectionId.put(cornerKey, intersectionId);
                }

                if (!intersectionIdToPosition.containsKey(intersectionId)) {
                    Map<String, Integer> position = new HashMap<>();
                    position.put("hexId", hexId);
                    position.put("corner", corner);
                    intersectionIdToPosition.put(intersectionId, position);
                }
            }
        }

        return intersectionIdToPosition;
    }

    private Map<Integer, String> buildEdgeIdToRoadPositionMap() {
        Map<String, Integer> cornerKeyToIntersectionId = new LinkedHashMap<>();
        Map<String, Integer> edgeKeyToEdgeId = new LinkedHashMap<>();
        Map<Integer, String> edgeIdToRoadPosition = new HashMap<>();
        int nextIntersectionId = 0;
        int nextEdgeId = 0;

        for (int hexId = 1; hexId <= 19; hexId++) {
            double[] center = toPixel(hexId);
            String[] cornerKeys = new String[6];

            for (int corner = 0; corner < 6; corner++) {
                double[] point = getCornerPoint(center[0], center[1], corner);
                String cornerKey = formatPoint(point[0], point[1]);
                cornerKeys[corner] = cornerKey;
                if (!cornerKeyToIntersectionId.containsKey(cornerKey)) {
                    cornerKeyToIntersectionId.put(cornerKey, nextIntersectionId++);
                }
            }

            for (int edge = 0; edge < 6; edge++) {
                Integer a = cornerKeyToIntersectionId.get(cornerKeys[edge]);
                Integer b = cornerKeyToIntersectionId.get(cornerKeys[(edge + 1) % 6]);
                if (a == null || b == null) {
                    continue;
                }

                String edgeKey = createCanonicalEdgeKey(a, b);
                Integer edgeId = edgeKeyToEdgeId.get(edgeKey);
                if (edgeId == null) {
                    edgeId = nextEdgeId++;
                    edgeKeyToEdgeId.put(edgeKey, edgeId);
                }

                if (!edgeIdToRoadPosition.containsKey(edgeId)) {
                    edgeIdToRoadPosition.put(edgeId, hexId + ":" + edge);
                }
            }
        }

        return edgeIdToRoadPosition;
    }

    private String createCanonicalEdgeKey(int intersectionAId, int intersectionBId) {
        int min = Math.min(intersectionAId, intersectionBId);
        int max = Math.max(intersectionAId, intersectionBId);
        return min + "|" + max;
    }

    private double[] toPixel(int hexId) {
        double[] coordinates = boardCoordinatesForHex(hexId);
        return new double[] {
            ORIGIN_X + coordinates[0] * HEX_SPACING_X,
            ORIGIN_Y + coordinates[1] * HEX_SPACING_Y
        };
    }

    private double[] boardCoordinatesForHex(int hexId) {
        return switch (hexId) {
            case 1 -> new double[] {1, 0};
            case 2 -> new double[] {2, 0};
            case 3 -> new double[] {3, 0};
            case 4 -> new double[] {0.5, 1};
            case 5 -> new double[] {1.5, 1};
            case 6 -> new double[] {2.5, 1};
            case 7 -> new double[] {3.5, 1};
            case 8 -> new double[] {0, 2};
            case 9 -> new double[] {1, 2};
            case 10 -> new double[] {2, 2};
            case 11 -> new double[] {3, 2};
            case 12 -> new double[] {4, 2};
            case 13 -> new double[] {0.5, 3};
            case 14 -> new double[] {1.5, 3};
            case 15 -> new double[] {2.5, 3};
            case 16 -> new double[] {3.5, 3};
            case 17 -> new double[] {1, 4};
            case 18 -> new double[] {2, 4};
            case 19 -> new double[] {3, 4};
            default -> throw new IllegalArgumentException("Unsupported hex id: " + hexId);
        };
    }

    private double[] getCornerPoint(double centerX, double centerY, int cornerIndex) {
        double angle = (Math.PI / 3.0) * cornerIndex + Math.PI / 6.0;
        return new double[] {
            centerX + HEX_SIZE * Math.cos(angle),
            centerY + HEX_SIZE * Math.sin(angle)
        };
    }

    private String formatPoint(double x, double y) {
        return Math.round(x) + ":" + Math.round(y);
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
