package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoatGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;

@Service
@Transactional
public class GameService {

    private static final double HEX_SIZE = 58.0;
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double ORIGIN_X = 150.0;
    private static final double ORIGIN_Y = 130.0;
    private static final double HEX_SPACING_X = HEX_SIZE * SQRT_3;
    private static final double HEX_SPACING_Y = HEX_SIZE * 1.5;

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    private static final class RoadSegment {
        private final String from;
        private final String to;
        private final String key;

        private RoadSegment(String from, String to) {
            this.from = from;
            this.to = to;
            this.key = from + "|" + to;
        }
    }

    public GameService(@Qualifier("gameRepository") GameRepository gameRepository,
                       @Qualifier("userRepository") UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    public Game createGame(String playerToken, GamePostDTO gamePostDTO) {
        authenticate(playerToken);

        Game game = new Game();
        Board board = resolveBoard(gamePostDTO);
        game.setBoard(board);
        game.setPlayers(convertPlayerDtosToEntity(gamePostDTO == null ? null : gamePostDTO.getPlayers()));
        game.setCurrentTurnIndex(gamePostDTO == null ? null : gamePostDTO.getCurrentTurnIndex());
        game.setTurnPhase(gamePostDTO == null || gamePostDTO.getTurnPhase() == null 
            ? TurnPhase.ROLL_DICE.toString() 
            : gamePostDTO.getTurnPhase());
        game.setDiceValue(gamePostDTO == null ? null : gamePostDTO.getDiceValue());
        game.setRobberTileIndex(resolveRobberTileIndex(board, gamePostDTO));
        game.setTargetVictoryPoints(resolveTargetVictoryPoints(gamePostDTO));
        game.setStartedAt(gamePostDTO == null ? null : gamePostDTO.getStartedAt());
        game.setFinishedAt(gamePostDTO == null ? null : gamePostDTO.getFinishedAt());
        game.setWinner(convertPlayerDtoToEntity(gamePostDTO == null ? null : gamePostDTO.getWinner()));
        game.setChatMessages(gamePostDTO == null || gamePostDTO.getChatMessages() == null
            ? Collections.emptyList()
            : new ArrayList<>(gamePostDTO.getChatMessages()));

        recalculateVictoryState(game);

        return gameRepository.save(game);
    }

    public Game updateGameState(Long gameId, String playerToken, GamePostDTO gamePostDTO) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Game with id " + gameId + " was not found."));

        if (gamePostDTO != null) {
            if (gamePostDTO.getBoard() != null) {
                Board board = convertBoardDtoToEntity(gamePostDTO.getBoard());
                game.setBoard(board);
                game.setRobberTileIndex(resolveRobberTileIndex(board, gamePostDTO));
            } else if (gamePostDTO.getRobberTileIndex() != null) {
                game.setRobberTileIndex(gamePostDTO.getRobberTileIndex());
            }

            if (gamePostDTO.getPlayers() != null) {
                game.setPlayers(convertPlayerDtosToEntity(gamePostDTO.getPlayers()));
            }

            if (gamePostDTO.getCurrentTurnIndex() != null) {
                game.setCurrentTurnIndex(gamePostDTO.getCurrentTurnIndex());
            }

            if (gamePostDTO.getTurnPhase() != null) {
                game.setTurnPhase(gamePostDTO.getTurnPhase());
            }

            if (gamePostDTO.getDiceValue() != null) {
                game.setDiceValue(gamePostDTO.getDiceValue());
            }

            if (gamePostDTO.getTargetVictoryPoints() != null) {
                game.setTargetVictoryPoints(gamePostDTO.getTargetVictoryPoints());
            }

            if (gamePostDTO.getStartedAt() != null) {
                game.setStartedAt(gamePostDTO.getStartedAt());
            }
        }

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game getGameById(Long gameId, String playerToken) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Game with id " + gameId + " was not found."));

        ensureBoardInitialized(game);
        return game;
    }

    public Board getBoardById(Long gameId, String playerToken) {
        Game game = getGameById(gameId, playerToken);
        ensureBoardInitialized(game);
        return game.getBoard();
    }

    public void appendChatMessage(Long gameId, String playerToken, String message) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<String> chatMessages = new ArrayList<>(
            Optional.ofNullable(game.getChatMessages()).orElse(Collections.emptyList())
        );
        chatMessages.add(message);
        game.setChatMessages(chatMessages);
        gameRepository.save(game);
    }

    public Game addRoadToPlayer(Long gameId, String playerToken, Long playerId, Integer hexId, Integer edge) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            return game;
        }

        String roadKey = String.format("%d:%d", hexId, edge);
        boolean occupied = players.stream()
            .filter(Objects::nonNull)
            .flatMap(player -> Optional.ofNullable(player.getRoadsOnEdges()).orElse(Collections.emptyList()).stream())
            .anyMatch(roadKey::equals);
        if (occupied) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Road edge is already occupied.");
        }

        boolean changed = false;

        for (Player player : players) {
            if (player == null || player.getId() == null || !playerId.equals(player.getId())) {
                continue;
            }

            if (!isConnectedToExistingRoad(player, hexId, edge)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Road must connect to one of your existing roads.");
            }

            int wood = resourceValue(player.getWood());
            int brick = resourceValue(player.getBrick());
            if (wood < 1 || brick < 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources to build a road.");
            }

            List<String> roads = new ArrayList<>(
                Optional.ofNullable(player.getRoadsOnEdges()).orElse(Collections.emptyList())
            );
            if (!roads.contains(roadKey)) {
                roads.add(roadKey);
                player.setRoadsOnEdges(roads);
                player.setWood(wood - 1);
                player.setBrick(brick - 1);
                changed = true;
            }
            break;
        }

        if (changed) {
            game.setPlayers(players);
            recalculateVictoryState(game);
            game = gameRepository.save(game);
        }

        return game;
    }

    private boolean isConnectedToExistingRoad(Player player, int hexId, int edge) {
        List<String> existingRoads = Optional.ofNullable(player.getRoadsOnEdges()).orElse(Collections.emptyList());
        if (existingRoads.isEmpty()) {
            return false;
        }

        String[] targetEndpoints = getCanonicalRoadEndpoints(hexId, edge);
        String targetFrom = targetEndpoints[0];
        String targetTo = targetEndpoints[1];

        for (String roadEntry : existingRoads) {
            RoadSegment segment = parseRoadSegment(roadEntry);
            if (segment == null) {
                continue;
            }

            if (segment.from.equals(targetFrom)
                    || segment.from.equals(targetTo)
                    || segment.to.equals(targetFrom)
                    || segment.to.equals(targetTo)) {
                return true;
            }
        }

        return false;
    }

    public Game applyBankTrade(Long gameId, String playerToken, Long sourcePlayerId,
            String giveResource, String receiveResource, Integer amount) {
        authenticate(playerToken);

        if (sourcePlayerId == null || giveResource == null || receiveResource == null || amount == null || amount < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bank trade payload.");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        Player source = findPlayerById(players, sourcePlayerId);
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source player was not found.");
        }

        int giveAmount = amount * 4;
        int currentGive = getResourceByName(source, giveResource);
        int currentReceive = getResourceByName(source, receiveResource);
        if (currentGive < giveAmount) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources for bank trade.");
        }

        setResourceByName(source, giveResource, currentGive - giveAmount);
        setResourceByName(source, receiveResource, currentReceive + amount);

        game.setPlayers(players);
        return gameRepository.save(game);
    }

    public Game applyPlayerTrade(Long gameId, String playerToken, Long sourcePlayerId, Long targetPlayerId,
            String giveResource, String receiveResource, Integer amount) {
        authenticate(playerToken);

        if (sourcePlayerId == null || targetPlayerId == null || giveResource == null || receiveResource == null || amount == null || amount < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        Player source = findPlayerById(players, sourcePlayerId);
        Player target = findPlayerById(players, targetPlayerId);
        if (source == null || target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade player was not found.");
        }

        int sourceGive = getResourceByName(source, giveResource);
        int sourceReceive = getResourceByName(source, receiveResource);
        int targetGive = getResourceByName(target, giveResource);
        int targetReceive = getResourceByName(target, receiveResource);

        if (sourceGive < amount || targetReceive < amount) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources for player trade.");
        }

        setResourceByName(source, giveResource, sourceGive - amount);
        setResourceByName(source, receiveResource, sourceReceive + amount);
        setResourceByName(target, receiveResource, targetReceive - amount);
        setResourceByName(target, giveResource, targetGive + amount);

        game.setPlayers(players);
        return gameRepository.save(game);
    }

    public Game rollDice(Long gameId, String playerToken) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        Player currentPlayer = getCurrentPlayer(game);
        if (currentPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No current player found.");
        }

        String currentPhase = game.getTurnPhase();
        if (!TurnPhase.ROLL_DICE.toString().equals(currentPhase)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot roll dice. Current phase is: " + currentPhase + ". Must be in ROLL_DICE phase.");
        }

        int die1 = 1 + (int) (Math.random() * 6);
        int die2 = 1 + (int) (Math.random() * 6);
        int diceSum = die1 + die2;

        game.setDiceValue(diceSum);
        game.setTurnPhase(TurnPhase.ACTION);

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game endTurn(Long gameId, String playerToken) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        Player currentPlayer = getCurrentPlayer(game);
        if (currentPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No current player found.");
        }

        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has no players.");
        }

        Integer nextTurnIndex = (game.getCurrentTurnIndex() + 1) % players.size();
        game.setCurrentTurnIndex(nextTurnIndex);
        game.setTurnPhase(TurnPhase.ROLL_DICE);
        game.setDiceValue(null);

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Player getCurrentPlayer(Game game) {
        if (game == null || game.getPlayers() == null || game.getPlayers().isEmpty()) {
            return null;
        }

        Integer turnIndex = game.getCurrentTurnIndex();
        if (turnIndex == null || turnIndex < 0 || turnIndex >= game.getPlayers().size()) {
            return null;
        }

        return game.getPlayers().get(turnIndex);
    }

    private void updateLongestRoadOwnership(List<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }

        Player currentHolder = players.stream()
            .filter(player -> player != null && Boolean.TRUE.equals(player.getHasLongestRoad()))
            .findFirst()
            .orElse(null);

        int highestRoadCount = players.stream()
            .filter(Objects::nonNull)
            .mapToInt(this::longestRoadLength)
            .max()
            .orElse(0);

        if (highestRoadCount < 5) {
            players.forEach(player -> {
                if (player != null) {
                    player.setHasLongestRoad(false);
                }
            });
            return;
        }

        List<Player> leaders = players.stream()
            .filter(player -> player != null && longestRoadLength(player) == highestRoadCount)
            .collect(Collectors.toList());

        Player holder = null;
        if (currentHolder != null && longestRoadLength(currentHolder) == highestRoadCount) {
            holder = currentHolder;
        } else if (leaders.size() == 1) {
            holder = leaders.get(0);
        }

        final Long holderId = holder == null ? null : holder.getId();
        players.forEach(player -> {
            if (player == null) {
                return;
            }

            player.setHasLongestRoad(holderId != null && holderId.equals(player.getId()));
        });
    }

    private int longestRoadLength(Player player) {
        if (player == null) {
            return 0;
        }

        List<String> roadEntries = Optional.ofNullable(player.getRoadsOnEdges()).orElse(Collections.emptyList());
        if (roadEntries.isEmpty()) {
            return 0;
        }

        List<RoadSegment> segments = new ArrayList<>();
        for (String roadEntry : roadEntries) {
            RoadSegment segment = parseRoadSegment(roadEntry);
            if (segment != null && segments.stream().noneMatch(existing -> existing.key.equals(segment.key))) {
                segments.add(segment);
            }
        }

        if (segments.isEmpty()) {
            return 0;
        }

        List<String> nodes = new ArrayList<>();
        for (RoadSegment segment : segments) {
            if (!nodes.contains(segment.from)) {
                nodes.add(segment.from);
            }
            if (!nodes.contains(segment.to)) {
                nodes.add(segment.to);
            }
        }

        boolean[] used = new boolean[segments.size()];
        int longest = 0;
        for (String node : nodes) {
            longest = Math.max(longest, depthFirstRoadLength(node, segments, used));
        }

        return longest;
    }

    private int depthFirstRoadLength(String node, List<RoadSegment> segments, boolean[] used) {
        int best = 0;

        for (int i = 0; i < segments.size(); i++) {
            if (used[i]) {
                continue;
            }

            RoadSegment segment = segments.get(i);
            if (!segment.from.equals(node) && !segment.to.equals(node)) {
                continue;
            }

            used[i] = true;
            String nextNode = segment.from.equals(node) ? segment.to : segment.from;
            best = Math.max(best, 1 + depthFirstRoadLength(nextNode, segments, used));
            used[i] = false;
        }

        return best;
    }

    private RoadSegment parseRoadSegment(String roadEntry) {
        if (roadEntry == null || roadEntry.isBlank()) {
            return null;
        }

        String[] parts = roadEntry.split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            int hexId = Integer.parseInt(parts[0]);
            int edge = Integer.parseInt(parts[1]);
            String[] endpoints = getCanonicalRoadEndpoints(hexId, edge);
            return new RoadSegment(endpoints[0], endpoints[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String[] getCanonicalRoadEndpoints(int hexId, int edge) {
        double[] center = toPixel(hexId);
        double[] point1 = getCornerPoint(center[0], center[1], edge);
        double[] point2 = getCornerPoint(center[0], center[1], (edge + 1) % 6);

        String a = formatPoint(point1[0], point1[1]);
        String b = formatPoint(point2[0], point2[1]);
        return a.compareTo(b) < 0 ? new String[] { a, b } : new String[] { b, a };
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

    private void ensureBoardInitialized(Game game) {
        Board board = game.getBoard();
        if (board != null
            && board.getHexTiles() != null
            && board.getHexTile_DiceNumbers() != null
            && board.getHexTiles().size() >= 19
            && board.getHexTile_DiceNumbers().size() >= 19) {
            return;
        }

        Board regeneratedBoard = new Board();
        regeneratedBoard.generateBoard();
        game.setBoard(regeneratedBoard);

        int desertIndex = regeneratedBoard.getHexTiles() != null ? regeneratedBoard.getHexTiles().indexOf("DESERT") : -1;
        game.setRobberTileIndex(desertIndex >= 0 ? desertIndex + 1 : null);
        gameRepository.save(game);
    }

    private Board resolveBoard(GamePostDTO gamePostDTO) {
        if (gamePostDTO != null && gamePostDTO.getBoard() != null) {
            return convertBoardDtoToEntity(gamePostDTO.getBoard());
        }

        Board board = new Board();
        board.generateBoard();
        return board;
    }

    private Board convertBoardDtoToEntity(BoardGetDTO boardGetDTO) {
        Board board = new Board();
        board.setHexTiles(boardGetDTO.getHexTiles());
        board.setIntersections(boardGetDTO.getIntersections());
        board.setEdges(boardGetDTO.getEdges());
        board.setPorts(boardGetDTO.getPorts());
        board.setBoats(convertBoatDtosToEntity(boardGetDTO.getBoats()));
        board.setHexTile_DiceNumbers(boardGetDTO.getHexTile_DiceNumbers());
        return board;
    }

    private List<Boat> convertBoatDtosToEntity(List<BoatGetDTO> boats) {
        if (boats == null) {
            return Collections.emptyList();
        }

        return boats.stream().map(this::convertBoatDtoToEntity).collect(Collectors.toList());
    }

    private Boat convertBoatDtoToEntity(BoatGetDTO boatDto) {
        Boat boat = new Boat();
        boat.setId(boatDto.getId());
        boat.setBoatType(boatDto.getBoatType());
        boat.setHexId(boatDto.getHexId());
        boat.setFirstCorner(boatDto.getFirstCorner());
        boat.setSecondCorner(boatDto.getSecondCorner());
        return boat;
    }

    private Integer resolveRobberTileIndex(Board board, GamePostDTO gamePostDTO) {
        if (gamePostDTO != null && gamePostDTO.getRobberTileIndex() != null) {
            return gamePostDTO.getRobberTileIndex();
        }

        if (board == null || board.getHexTiles() == null) {
            return null;
        }

        int desertIndex = board.getHexTiles().indexOf("DESERT");
        return desertIndex >= 0 ? desertIndex + 1 : null;
    }

    private Integer resolveTargetVictoryPoints(GamePostDTO gamePostDTO) {
        if (gamePostDTO == null || gamePostDTO.getTargetVictoryPoints() == null || gamePostDTO.getTargetVictoryPoints() < 1) {
            return 10;
        }

        return gamePostDTO.getTargetVictoryPoints();
    }

    private List<Player> convertPlayerDtosToEntity(List<PlayerGetDTO> players) {
        if (players == null) {
            return Collections.emptyList();
        }

        return players.stream().map(this::convertPlayerDtoToEntity).collect(Collectors.toList());
    }

    private Player convertPlayerDtoToEntity(PlayerGetDTO playerDto) {
        if (playerDto == null) {
            return null;
        }

        Player player = new Player();
        player.setId(playerDto.getId());
        player.setName(playerDto.getName());
        player.setSettlementPoints(playerDto.getSettlementPoints());
        player.setCityPoints(playerDto.getCityPoints());
        player.setDevelopmentCardVictoryPoints(playerDto.getDevelopmentCardVictoryPoints());
        player.setHasLongestRoad(playerDto.getHasLongestRoad());
        player.setHasLargestArmy(playerDto.getHasLargestArmy());
        player.setRoadsOnEdges(playerDto.getRoadsOnEdges());
        player.setWood(playerDto.getWood());
        player.setBrick(playerDto.getBrick());
        player.setWool(playerDto.getWool());
        player.setWheat(playerDto.getWheat());
        player.setOre(playerDto.getOre());
        player.recalculateVictoryPoints();
        return player;
    }

    private void recalculateVictoryState(Game game) {
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            game.setWinner(null);
            game.setFinishedAt(null);
            return;
        }

        updateLongestRoadOwnership(players);

        players.forEach(player -> {
            if (player != null) {
                player.recalculateVictoryPoints();
            }
        });

        int targetVictoryPoints = safeInt(game.getTargetVictoryPoints(), 10);
        Comparator<Player> rankingComparator = Comparator
            .comparingInt((Player player) -> safeInt(player == null ? null : player.getVictoryPoints(), 0))
            .thenComparingInt(player -> safeInt(player == null ? null : player.getCityPoints(), 0))
            .thenComparingInt(player -> safeInt(player == null ? null : player.getSettlementPoints(), 0))
            .thenComparingLong(player -> safeLong(player == null ? null : player.getId(), Long.MAX_VALUE));

        Player topPlayer = players.stream().max(rankingComparator).orElse(null);

        if (topPlayer != null && topPlayer.getVictoryPoints() >= targetVictoryPoints) {
            game.setWinner(topPlayer);
            if (game.getFinishedAt() == null) {
                game.setFinishedAt(LocalDateTime.now());
            }
        } else {
            game.setWinner(null);
            game.setFinishedAt(null);
        }
    }

    private User authenticate(String playerToken) {
        if (playerToken == null || playerToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token.");
        }

        User user = userRepository.findByToken(playerToken);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization token.");
        }
        return user;
    }

    private int safeInt(Integer value, int fallback) {
        return Optional.ofNullable(value).orElse(fallback);
    }

    private long safeLong(Long value, long fallback) {
        return Optional.ofNullable(value).orElse(fallback);
    }

    private Player findPlayerById(List<Player> players, Long playerId) {
        if (players == null || playerId == null) {
            return null;
        }

        return players.stream()
            .filter(player -> player != null && playerId.equals(player.getId()))
            .findFirst()
            .orElse(null);
    }

    private int resourceValue(Integer value) {
        return Optional.ofNullable(value).orElse(10);
    }

    private int getResourceByName(Player player, String resource) {
        return switch (normalizeResourceName(resource)) {
            case "wood" -> resourceValue(player.getWood());
            case "brick" -> resourceValue(player.getBrick());
            case "wool" -> resourceValue(player.getWool());
            case "wheat" -> resourceValue(player.getWheat());
            case "ore" -> resourceValue(player.getOre());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported resource: " + resource);
        };
    }

    private void setResourceByName(Player player, String resource, int value) {
        int normalized = Math.max(0, value);
        switch (normalizeResourceName(resource)) {
            case "wood" -> player.setWood(normalized);
            case "brick" -> player.setBrick(normalized);
            case "wool" -> player.setWool(normalized);
            case "wheat" -> player.setWheat(normalized);
            case "ore" -> player.setOre(normalized);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported resource: " + resource);
        }
    }

    private String normalizeResourceName(String resource) {
        return resource == null ? "" : resource.trim().toLowerCase();
    }
}