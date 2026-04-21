package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import ch.uzh.ifi.hase.soprafs26.entity.Building;
import ch.uzh.ifi.hase.soprafs26.entity.City;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
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

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

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

    public Game addRoadToPlayer(Long gameId, String playerToken, Long playerId, Integer edgeId) {
        authenticate(playerToken);

        if (playerId == null || edgeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player id and edge id are required.");
        }    

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            return game;
        }

        Edge targetEdge = findEdgeById(game, edgeId);
        if (targetEdge == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Edge with id " + edgeId + " was not found.");
        }

        if (targetEdge.isOccupied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Road edge is already occupied.");
        }

        Player player = findPlayerById(players, playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player with id " + playerId + " was not found.");
        }

        boolean connectedToOwnBuilding =
            hasOwnBuildingAtIntersection(game, targetEdge.getIntersectionAId(), playerId)
            || hasOwnBuildingAtIntersection(game, targetEdge.getIntersectionBId(), playerId);
        
        boolean connectedToOwnRoad =
            hasOwnRoadAtIntersection(game, targetEdge.getIntersectionAId(), playerId)
            || hasOwnRoadAtIntersection(game, targetEdge.getIntersectionBId(), playerId);
        
        if (!connectedToOwnBuilding && !connectedToOwnRoad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Road must connect to one of the player's buildings or roads.");
        }

        int wood = resourceValue(player.getWood());
        int brick = resourceValue(player.getBrick());
        if (wood < 1 || brick < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources to build a road.");
        }

        Road road = new Road();
        road.setOwnerPlayerId(playerId);
        road.setEdgeId(edgeId);
    
        targetEdge.setRoad(road);
        player.setWood(wood - 1);
        player.setBrick(brick - 1);
    
        game.setPlayers(players);
        recalculateVictoryState(game);
    
        return gameRepository.save(game);
    }

    public Game addSettlementToPlayer(Long gameId, String playerToken, Long playerId, Integer intersectionId) {
        authenticate(playerToken);
    
        if (playerId == null || intersectionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player id and intersection id are required.");
        }
    
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));
    
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            return game;
        }
    
        Intersection intersection = findIntersectionById(game, intersectionId);
        if (intersection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Intersection with id " + intersectionId + " was not found.");
        }
    
        if (intersection.isOccupied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Intersection is already occupied.");
        }

        if (hasAdjacentBuilding(game, intersectionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A settlement cannot be built next to another building.");
        }
    
        Player player = findPlayerById(players, playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Player with id " + playerId + " was not found.");
        }

        if (!hasOwnRoadLeadingToIntersection(game, intersectionId, playerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Settlement must connect to one of the player's roads.");
        }
    
        int wood = resourceValue(player.getWood());
        int brick = resourceValue(player.getBrick());
        int wool = resourceValue(player.getWool());
        int wheat = resourceValue(player.getWheat());
    
        if (wood < 1 || brick < 1 || wool < 1 || wheat < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Not enough resources to build a settlement.");
        }
    
        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(playerId);
        settlement.setIntersectionId(intersectionId);
    
        intersection.setBuilding(settlement);
    
        player.setWood(wood - 1);
        player.setBrick(brick - 1);
        player.setWool(wool - 1);
        player.setWheat(wheat - 1);
        player.setSettlementPoints(safeInt(player.getSettlementPoints(), 0) + 1);
    
        game.setPlayers(players);
        recalculateVictoryState(game);
    
        return gameRepository.save(game);
    }

    public Game upgradeSettlementToCity(Long gameId, String playerToken, Long playerId, Integer intersectionId) {
        authenticate(playerToken);
    
        if (playerId == null || intersectionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player id and intersection id are required.");
        }
    
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));
    
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            return game;
        }
    
        Intersection intersection = findIntersectionById(game, intersectionId);
        if (intersection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Intersection with id " + intersectionId + " was not found.");
        }
    
        if (!(intersection.getBuilding() instanceof Settlement)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "There is no settlement to upgrade at this intersection.");
        }
    
        Settlement settlement = (Settlement) intersection.getBuilding();
        if (settlement.getOwnerPlayerId() == null || !settlement.getOwnerPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "The settlement at this intersection does not belong to this player.");
        }
    
        Player player = findPlayerById(players, playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Player with id " + playerId + " was not found.");
        }
    
        int wheat = resourceValue(player.getWheat());
        int ore = resourceValue(player.getOre());
    
        if (wheat < 2 || ore < 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Not enough resources to build a city.");
        }
    
        City city = new City();
        city.setOwnerPlayerId(playerId);
        city.setIntersectionId(intersectionId);
    
        intersection.setBuilding(city);
    
        player.setWheat(wheat - 2);
        player.setOre(ore - 3);
        player.setSettlementPoints(Math.max(0, safeInt(player.getSettlementPoints(), 0) - 1));
        player.setCityPoints(safeInt(player.getCityPoints(), 0) + 1);
    
        game.setPlayers(players);
        recalculateVictoryState(game);
    
        return gameRepository.save(game);
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
        User authenticatedUser = authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        Player currentPlayer = getCurrentPlayer(game);
        if (currentPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No current player found.");
        }

        ensureCurrentPlayerCanRollDice(currentPlayer, authenticatedUser);

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

    private void updateLongestRoadOwnership(Game game) {
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            return;
        }
    
        Player currentHolder = players.stream()
            .filter(player -> player != null && Boolean.TRUE.equals(player.getHasLongestRoad()))
            .findFirst()
            .orElse(null);
    
        int highestRoadCount = players.stream()
            .filter(Objects::nonNull)
            .mapToInt(player -> longestRoadLength(game, player))
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
            .filter(player -> player != null && longestRoadLength(game, player) == highestRoadCount)
            .collect(Collectors.toList());
    
        Player holder = null;
        if (currentHolder != null && longestRoadLength(game, currentHolder) == highestRoadCount) {
            holder = currentHolder;
        } else if (leaders.size() == 1) {
            holder = leaders.get(0);
        }
    
        final Long holderId = holder == null ? null : holder.getId();
        players.forEach(player -> {
            if (player != null) {
                player.setHasLongestRoad(holderId != null && holderId.equals(player.getId()));
            }
        });
    }

    private int longestRoadLength(Game game, Player player) {
        if (game == null || player == null || player.getId() == null) {
            return 0;
        }
    
        if (game.getBoard() == null || game.getBoard().getEdges() == null) {
            return 0;
        }
    
        List<Edge> playerEdges = game.getBoard().getEdges().stream()
            .filter(Objects::nonNull)
            .filter(edge -> edge.getRoad() != null)
            .filter(edge -> edge.getRoad().getOwnerPlayerId() != null)
            .filter(edge -> edge.getRoad().getOwnerPlayerId().equals(player.getId()))
            .filter(edge -> edge.getIntersectionAId() != null && edge.getIntersectionBId() != null)
            .toList();
    
        if (playerEdges.isEmpty()) {
            return 0;
        }
    
        Map<Integer, List<Edge>> adjacency = new java.util.HashMap<>();
    
        for (Edge edge : playerEdges) {
            adjacency.computeIfAbsent(edge.getIntersectionAId(), key -> new ArrayList<>()).add(edge);
            adjacency.computeIfAbsent(edge.getIntersectionBId(), key -> new ArrayList<>()).add(edge);
        }
    
        int longest = 0;
    
        for (Integer startIntersectionId : adjacency.keySet()) {
            longest = Math.max(
                longest,
                longestRoadFromIntersection(game, startIntersectionId, player.getId(), adjacency, new ArrayList<>())
            );
        }
    
        return longest;
    }

    private int longestRoadFromIntersection(Game game, Integer currentIntersectionId, Long playerId,
        Map<Integer, List<Edge>> adjacency, List<Integer> usedEdgeIds) {

        if (currentIntersectionId == null) {
            return 0;
        }

        int best = 0;
        List<Edge> connectedEdges = adjacency.getOrDefault(currentIntersectionId, Collections.emptyList());

        for (Edge edge : connectedEdges) {
            if (edge == null || edge.getId() == null) {
                continue;
            }

            if (usedEdgeIds.contains(edge.getId())) {
                continue;
            }

            Integer nextIntersectionId = getOtherIntersectionId(edge, currentIntersectionId);
            if (nextIntersectionId == null) {
                continue;
            }

            List<Integer> nextUsedEdgeIds = new ArrayList<>(usedEdgeIds);
            nextUsedEdgeIds.add(edge.getId());

            int candidateLength = 1;

            if (!isBlockedByOpponentBuilding(game, nextIntersectionId, playerId)) {
                candidateLength += longestRoadFromIntersection(
                    game,
                    nextIntersectionId,
                    playerId,
                    adjacency,
                    nextUsedEdgeIds
                );
            }

            best = Math.max(best, candidateLength);
        }

        return best;
    }

    private Integer getOtherIntersectionId(Edge edge, Integer currentIntersectionId) {
        if (edge == null || currentIntersectionId == null) {
            return null;
        }
    
        if (currentIntersectionId.equals(edge.getIntersectionAId())) {
            return edge.getIntersectionBId();
        }
    
        if (currentIntersectionId.equals(edge.getIntersectionBId())) {
            return edge.getIntersectionAId();
        }
    
        return null;
    }

    private boolean isBlockedByOpponentBuilding(Game game, Integer intersectionId, Long playerId) {
        Intersection intersection = findIntersectionById(game, intersectionId);
    
        if (intersection == null || intersection.getBuilding() == null) {
            return false;
        }
    
        Building building = intersection.getBuilding();
        Long ownerPlayerId = building.getOwnerPlayerId();
    
        return ownerPlayerId != null && !ownerPlayerId.equals(playerId);
    }

    private boolean hasOwnBuildingAtIntersection(Game game, Integer intersectionId, Long playerId) {
        Intersection intersection = findIntersectionById(game, intersectionId);
    
        if (intersection == null || intersection.getBuilding() == null || playerId == null) {
            return false;
        }
    
        Long ownerPlayerId = intersection.getBuilding().getOwnerPlayerId();
        return ownerPlayerId != null && ownerPlayerId.equals(playerId);
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

        updateLongestRoadOwnership(game);

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

    private void ensureCurrentPlayerCanRollDice(Player currentPlayer, User authenticatedUser) {
        if (currentPlayer == null || authenticatedUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the active player can roll dice.");
        }

        if (isSameUserById(currentPlayer, authenticatedUser) || isSameUserByName(currentPlayer, authenticatedUser)) {
            return;
        }

        if (!hasResolvableCurrentPlayerIdentity(currentPlayer)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the active player can roll dice.");
    }

    private boolean isSameUserById(Player currentPlayer, User authenticatedUser) {
        if (currentPlayer.getUser() == null || currentPlayer.getUser().getId() == null || authenticatedUser.getId() == null) {
            return false;
        }

        return currentPlayer.getUser().getId().equals(authenticatedUser.getId());
    }

    private boolean isSameUserByName(Player currentPlayer, User authenticatedUser) {
        if (currentPlayer.getName() == null || authenticatedUser.getUsername() == null) {
            return false;
        }

        return currentPlayer.getName().equals(authenticatedUser.getUsername());
    }

    private boolean hasResolvableCurrentPlayerIdentity(Player currentPlayer) {
        if (currentPlayer.getUser() != null && currentPlayer.getUser().getId() != null) {
            return true;
        }

        return currentPlayer.getName() != null;
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
        return Optional.ofNullable(value).orElse(0);
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

    private Intersection findIntersectionById(Game game, Integer intersectionId) {
        if (game == null || intersectionId == null || game.getBoard() == null || game.getBoard().getIntersections() == null) {
            return null;
        }
    
        return game.getBoard().getIntersections().stream()
                .filter(Objects::nonNull)
                .filter(intersection -> intersectionId.equals(intersection.getId()))
                .findFirst()
                .orElse(null);
    }
    
    private Edge findEdgeById(Game game, Integer edgeId) {
        if (game == null || edgeId == null || game.getBoard() == null || game.getBoard().getEdges() == null) {
            return null;
        }
    
        return game.getBoard().getEdges().stream()
                .filter(Objects::nonNull)
                .filter(edge -> edgeId.equals(edge.getId()))
                .findFirst()
                .orElse(null);
    }

    private boolean hasAdjacentBuilding(Game game, Integer intersectionId) {
        if (game == null || intersectionId == null || game.getBoard() == null || game.getBoard().getEdges() == null) {
            return false;
        }
    
        for (Edge edge : game.getBoard().getEdges()) {
            if (edge == null || edge.getIntersectionAId() == null || edge.getIntersectionBId() == null) {
                continue;
            }
    
            Integer neighborId = null;
    
            if (intersectionId.equals(edge.getIntersectionAId())) {
                neighborId = edge.getIntersectionBId();
            } else if (intersectionId.equals(edge.getIntersectionBId())) {
                neighborId = edge.getIntersectionAId();
            }
    
            if (neighborId == null) {
                continue;
            }
    
            Intersection neighbor = findIntersectionById(game, neighborId);
            if (neighbor != null && neighbor.getBuilding() != null) {
                return true;
            }
        }
    
        return false;
    }

    private boolean hasOwnRoadAtIntersection(Game game, Integer intersectionId, Long playerId) {
        if (game == null || intersectionId == null || playerId == null
            || game.getBoard() == null || game.getBoard().getEdges() == null) {
            return false;
        }
    
        for (Edge edge : game.getBoard().getEdges()) {
            if (edge == null || edge.getRoad() == null) {
                continue;
            }
    
            if (!playerId.equals(edge.getRoad().getOwnerPlayerId())) {
                continue;
            }
    
            if (intersectionId.equals(edge.getIntersectionAId()) || intersectionId.equals(edge.getIntersectionBId())) {
                return true;
            }
        }
    
        return false;
    }

    private boolean hasOwnRoadLeadingToIntersection(Game game, Integer intersectionId, Long playerId) {
        return hasOwnRoadAtIntersection(game, intersectionId, playerId);
    }
}