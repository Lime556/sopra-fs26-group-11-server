package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
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
import ch.uzh.ifi.hase.soprafs26.entity.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoatGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RollDiceRequestDTO;

@Service
@Transactional
public class GameService {

    private static final int BANK_STARTING_RESOURCE_COUNT = 19;

    private static final String CARD_KNIGHT = "knight";
    private static final String CARD_VICTORY_POINT = "victory_point";
    private static final String CARD_ROAD_BUILDING = "road_building";
    private static final String CARD_YEAR_OF_PLENTY = "year_of_plenty";
    private static final String CARD_MONOPOLY = "monopoly";
    private static final List<String> TRADE_RESOURCES = List.of("wood", "brick", "wool", "wheat", "ore");

    private final GameRepository gameRepository;
    private final UserService userService;
    
    public GameService(
        @Qualifier("gameRepository") GameRepository gameRepository,
        UserService userService
    ) {
        this.gameRepository = gameRepository;
        this.userService = userService;
    }

    public Game createGame(String playerToken, GamePostDTO gamePostDTO) {
        User authenticatedUser = authenticate(playerToken);

        Game game = new Game();
        Board board = resolveBoard(gamePostDTO);
        game.setBoard(board);
        game.setPlayers(convertPlayerDtosToEntity(gamePostDTO == null ? null : gamePostDTO.getPlayers()));
        game.setCurrentTurnIndex(gamePostDTO == null ? null : gamePostDTO.getCurrentTurnIndex());
        game.setTurnPhase(gamePostDTO == null || gamePostDTO.getTurnPhase() == null 
            ? TurnPhase.ROLL_DICE.toString() 
            : gamePostDTO.getTurnPhase());
        game.setGamePhase(GamePhase.SETUP.toString());
        game.setDiceValue(gamePostDTO == null ? null : gamePostDTO.getDiceValue());
        game.setRobberTileIndex(resolveRobberTileIndex(board, gamePostDTO));
        initializeDevelopmentDeck(game, gamePostDTO == null ? null : gamePostDTO.getDevelopmentDeck());
        game.setTargetVictoryPoints(resolveTargetVictoryPoints(gamePostDTO));
        game.setStartedAt(gamePostDTO == null ? null : gamePostDTO.getStartedAt());
        game.setFinishedAt(gamePostDTO == null ? null : gamePostDTO.getFinishedAt());
        game.setWinner(convertPlayerDtoToEntity(gamePostDTO == null ? null : gamePostDTO.getWinner()));
        game.setChatMessages(gamePostDTO == null || gamePostDTO.getChatMessages() == null
            ? new ArrayList<>()
            : new ArrayList<>(gamePostDTO.getChatMessages()));
        initializeBankResources(game, gamePostDTO == null ? null : gamePostDTO.getBankResources());

        // If no players are provided (e.g. game created via direct board access), 
        // add the authenticated user as the first player.
        if (game.getPlayers().isEmpty()) {
            Player newPlayer = new Player();
            newPlayer.setUser(authenticatedUser);
            newPlayer.setName(authenticatedUser.getUsername());
            newPlayer.setColor("RED");
            newPlayer.setSettlementPoints(0);
            newPlayer.setCityPoints(0);
            newPlayer.setDevelopmentCardVictoryPoints(0);
            newPlayer.setWood(0); newPlayer.setBrick(0); newPlayer.setWool(0); newPlayer.setWheat(0); newPlayer.setOre(0);
            newPlayer.setKnightsPlayed(0);
            newPlayer.setFreeRoadBuildsRemaining(0);
            newPlayer.recalculateVictoryPoints();
            game.getPlayers().add(newPlayer);
            if (game.getCurrentTurnIndex() == null) {
                game.setCurrentTurnIndex(0);
            }
        }

        game = gameRepository.save(game);
        final Long savedGameId = game.getId();
        game.getPlayers().forEach(p -> p.setGameId(savedGameId));

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

            if (gamePostDTO.getDevelopmentDeck() != null) {
                initializeDevelopmentDeck(game, gamePostDTO.getDevelopmentDeck());
            }

            if (gamePostDTO.getStartedAt() != null) {
                game.setStartedAt(gamePostDTO.getStartedAt());
            }

            if (gamePostDTO.getBankResources() != null) {
                applyBankResourcesFromMap(game, gamePostDTO.getBankResources());
            }
        }

        ensureBankInitialized(game);

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game moveRobber(Long gameId, String token, Integer hexId) {
        authenticate(token);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        if (hexId == null || !isValidHexId(game, hexId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid hex id.");
        }

        if (hexId.equals(game.getRobberTileIndex())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move robber to the same tile.");
        }

        game.setRobberTileIndex(hexId);
        return gameRepository.save(game);
    }

    public Game getGameById(Long gameId, String playerToken) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Game with id " + gameId + " was not found."));

        ensureBoardInitialized(game);
        ensureBankInitialized(game);
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
        ensureBankInitialized(game);

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

        int freeRoadBuildsRemaining = safeInt(player.getFreeRoadBuildsRemaining(), 0);
        int wood = resourceValue(player.getWood());
        int brick = resourceValue(player.getBrick());
        if (freeRoadBuildsRemaining <= 0 && (wood < 1 || brick < 1)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources to build a road.");
        }

        Road road = new Road();
        road.setOwnerPlayerId(playerId);
        road.setEdgeId(edgeId);
    
        targetEdge.setRoad(road);
        if (freeRoadBuildsRemaining > 0) {
            player.setFreeRoadBuildsRemaining(freeRoadBuildsRemaining - 1);
        }
        else {
            player.setWood(wood - 1);
            player.setBrick(brick - 1);
            addToBank(game, "wood", 1);
            addToBank(game, "brick", 1);
        }
    
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
        ensureBankInitialized(game);
    
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
        addToBank(game, "wood", 1);
        addToBank(game, "brick", 1);
        addToBank(game, "wool", 1);
        addToBank(game, "wheat", 1);
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
        ensureBankInitialized(game);
    
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
        addToBank(game, "wheat", 2);
        addToBank(game, "ore", 3);
        player.setSettlementPoints(Math.max(0, safeInt(player.getSettlementPoints(), 0) - 1));
        player.setCityPoints(safeInt(player.getCityPoints(), 0) + 1);
    
        game.setPlayers(players);
        recalculateVictoryState(game);
    
        return gameRepository.save(game);
    }

    public Game applyBankTrade(Long gameId, String playerToken, GameEventDTO tradeEvent) {
        authenticate(playerToken);

        if (tradeEvent == null || tradeEvent.getSourcePlayerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bank trade payload.");
        }

        Map<String, Integer> giveBundle;
        Map<String, Integer> receiveBundle;
        if (tradeEvent.getGiveResources() != null || tradeEvent.getReceiveResources() != null) {
            giveBundle = normalizeTradeBundle(tradeEvent.getGiveResources());
            receiveBundle = normalizeTradeBundle(tradeEvent.getReceiveResources());
        } else {
            if (tradeEvent.getGiveResource() == null || tradeEvent.getReceiveResource() == null || tradeEvent.getAmount() == null || tradeEvent.getAmount() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bank trade payload.");
            }
            giveBundle = createSingleResourceBundle(tradeEvent.getGiveResource(), tradeEvent.getAmount() * 4);
            receiveBundle = createSingleResourceBundle(tradeEvent.getReceiveResource(), tradeEvent.getAmount());
        }

        int totalGive = sumTradeBundle(giveBundle);
        int totalReceive = sumTradeBundle(receiveBundle);
        if (totalGive < 1 || totalReceive < 1 || totalGive != totalReceive * 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bank trade payload.");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        Player source = findPlayerById(players, tradeEvent.getSourcePlayerId());
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source player was not found.");
        }
        ensureBankInitialized(game);

        for (String resource : TRADE_RESOURCES) {
            int giveAmount = giveBundle.get(resource);
            int receiveAmount = receiveBundle.get(resource);
            if (getResourceByName(source, resource) < giveAmount) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources for bank trade.");
            }
            if (getBankResourceByName(game, resource) < receiveAmount) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Bank does not have enough resources for this trade.");
            }
        }

        for (String resource : TRADE_RESOURCES) {
            int giveAmount = giveBundle.get(resource);
            int receiveAmount = receiveBundle.get(resource);
            if (giveAmount > 0) {
                setResourceByName(source, resource, getResourceByName(source, resource) - giveAmount);
                addToBank(game, resource, giveAmount);
            }
            if (receiveAmount > 0) {
                setResourceByName(source, resource, getResourceByName(source, resource) + receiveAmount);
                removeFromBank(game, resource, receiveAmount);
            }
        }

        game.setPlayers(players);
        return gameRepository.save(game);
    }

    public void validatePlayerTradeRequest(Long gameId, String playerToken, GameEventDTO tradeEvent) {
        User authenticatedUser = authenticate(playerToken);
        validatePlayerTradePayload(gameId, tradeEvent, false);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        Player source = findPlayerById(players, tradeEvent.getSourcePlayerId());
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade player was not found.");
        }

        ensurePlayerMatchesAuthenticatedUser(source, authenticatedUser, "request trade");
        // Verify source has enough resources for the requested give bundle
        Map<String, Integer> giveBundle;
        if (tradeEvent.getGiveResources() != null) {
            giveBundle = normalizeTradeBundle(tradeEvent.getGiveResources());
        } else {
            if (tradeEvent.getGiveResource() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
            }
            Integer giveAmount = tradeEvent.getGiveAmount() != null ? tradeEvent.getGiveAmount() : tradeEvent.getAmount();
            if (giveAmount == null || giveAmount < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
            }
            giveBundle = createSingleResourceBundle(tradeEvent.getGiveResource(), giveAmount);
        }

        for (String resource : TRADE_RESOURCES) {
            int giveAmount = giveBundle.get(resource);
            if (getResourceByName(source, resource) < giveAmount) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources for player trade.");
            }
        }
    }

    public void validatePlayerTradeResponse(Long gameId, String playerToken, GameEventDTO tradeEvent) {
        User authenticatedUser = authenticate(playerToken);
        validatePlayerTradePayload(gameId, tradeEvent, false);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        Player source = findPlayerById(players, tradeEvent.getSourcePlayerId());
        Player target = findPlayerById(players, tradeEvent.getTargetPlayerId());
        if (source == null || target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade player was not found.");
        }

        ensurePlayerMatchesAuthenticatedUser(target, authenticatedUser, "respond to trade request");

        if (tradeEvent.getTradeAction() != null && "ACCEPT".equalsIgnoreCase(tradeEvent.getTradeAction())) {
            Map<String, Integer> receiveBundle;
            if (tradeEvent.getReceiveResources() != null) {
                receiveBundle = normalizeTradeBundle(tradeEvent.getReceiveResources());
            } else {
                if (tradeEvent.getReceiveResource() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
                }

                Integer receiveAmount = tradeEvent.getReceiveAmount() != null ? tradeEvent.getReceiveAmount() : tradeEvent.getAmount();
                if (receiveAmount == null || receiveAmount < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
                }
                receiveBundle = createSingleResourceBundle(tradeEvent.getReceiveResource(), receiveAmount);
            }

            for (String resource : TRADE_RESOURCES) {
                int receiveAmount = receiveBundle.get(resource);
                if (getResourceByName(target, resource) < receiveAmount) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources for player trade.");
                }
            }
        }
    }

    public Game applyPlayerTrade(Long gameId, String playerToken, GameEventDTO tradeEvent) {
        User authenticatedUser = authenticate(playerToken);

        if (tradeEvent == null || tradeEvent.getSourcePlayerId() == null || tradeEvent.getTargetPlayerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
        }

        if (tradeEvent.getTradeAction() != null && "REQUEST".equalsIgnoreCase(tradeEvent.getTradeAction())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trade requests cannot be executed immediately.");
        }

        Map<String, Integer> giveBundle;
        Map<String, Integer> receiveBundle;
        if (tradeEvent.getGiveResources() != null || tradeEvent.getReceiveResources() != null) {
            giveBundle = normalizeTradeBundle(tradeEvent.getGiveResources());
            receiveBundle = normalizeTradeBundle(tradeEvent.getReceiveResources());
        } else {
            if (tradeEvent.getGiveResource() == null || tradeEvent.getReceiveResource() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
            }
            Integer giveAmount = tradeEvent.getGiveAmount() != null ? tradeEvent.getGiveAmount() : tradeEvent.getAmount();
            Integer receiveAmount = tradeEvent.getReceiveAmount() != null ? tradeEvent.getReceiveAmount() : tradeEvent.getAmount();
            if (giveAmount == null || receiveAmount == null || giveAmount < 1 || receiveAmount < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
            }
            giveBundle = createSingleResourceBundle(tradeEvent.getGiveResource(), giveAmount);
            receiveBundle = createSingleResourceBundle(tradeEvent.getReceiveResource(), receiveAmount);
        }

        if (sumTradeBundle(giveBundle) < 1 || sumTradeBundle(receiveBundle) < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        Player source = findPlayerById(players, tradeEvent.getSourcePlayerId());
        Player target = findPlayerById(players, tradeEvent.getTargetPlayerId());
        if (source == null || target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade player was not found.");
        }

        ensurePlayerMatchesAuthenticatedUser(source, authenticatedUser, "finalize trade");

        for (String resource : TRADE_RESOURCES) {
            int giveAmount = giveBundle.get(resource);
            int receiveAmount = receiveBundle.get(resource);
            if (getResourceByName(source, resource) < giveAmount || getResourceByName(target, resource) < receiveAmount) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources for player trade.");
            }
        }

        for (String resource : TRADE_RESOURCES) {
            int giveAmount = giveBundle.get(resource);
            int receiveAmount = receiveBundle.get(resource);

            if (giveAmount > 0) {
                setResourceByName(source, resource, getResourceByName(source, resource) - giveAmount);
                setResourceByName(target, resource, getResourceByName(target, resource) + giveAmount);
            }
            if (receiveAmount > 0) {
                setResourceByName(target, resource, getResourceByName(target, resource) - receiveAmount);
                setResourceByName(source, resource, getResourceByName(source, resource) + receiveAmount);
            }
        }

        game.setPlayers(players);
        return gameRepository.save(game);
    }

    private void validatePlayerTradePayload(Long gameId, GameEventDTO tradeEvent, boolean requireTarget) {
        if (tradeEvent == null || tradeEvent.getSourcePlayerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
        }

        if (requireTarget && tradeEvent.getTargetPlayerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
        }

        if (tradeEvent.getTradeAction() != null && "REQUEST".equalsIgnoreCase(tradeEvent.getTradeAction())) {
            if (tradeEvent.getGiveResources() != null || tradeEvent.getReceiveResources() != null) {
                normalizeTradeBundle(tradeEvent.getGiveResources());
                normalizeTradeBundle(tradeEvent.getReceiveResources());
                return;
            }

            if (tradeEvent.getGiveResource() == null || tradeEvent.getReceiveResource() == null || tradeEvent.getAmount() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
            }

            if (tradeEvent.getAmount() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player trade payload.");
            }
        }
    }

    private Map<String, Integer> normalizeTradeBundle(Map<String, Integer> bundle) {
        Map<String, Integer> normalized = new HashMap<>();
        for (String resource : TRADE_RESOURCES) {
            normalized.put(resource, 0);
        }

        if (bundle == null) {
            return normalized;
        }

        for (Map.Entry<String, Integer> entry : bundle.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            String resource = entry.getKey().toLowerCase(Locale.ROOT);
            if (!normalized.containsKey(resource)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trade resource payload.");
            }

            Integer amountValue = entry.getValue();
            int amount = amountValue != null ? amountValue.intValue() : 0;
            if (amount < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trade resource payload.");
            }

            normalized.put(resource, amount);
        }

        return normalized;
    }

    private Map<String, Integer> createSingleResourceBundle(String resource, int amount) {
        if (resource == null || amount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trade resource payload.");
        }

        Map<String, Integer> bundle = normalizeTradeBundle(null);
        String normalizedResource = resource.toLowerCase(Locale.ROOT);
        if (!bundle.containsKey(normalizedResource)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trade resource payload.");
        }

        bundle.put(normalizedResource, amount);
        return bundle;
    }

    private int sumTradeBundle(Map<String, Integer> bundle) {
        int sum = 0;
        for (String resource : TRADE_RESOURCES) {
            sum += Math.max(0, bundle.getOrDefault(resource, 0));
        }
        return sum;
    }

    private void ensurePlayerMatchesAuthenticatedUser(Player player, User authenticatedUser, String actionDescription) {
        if (player == null || authenticatedUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the targeted player can " + actionDescription + ".");
        }

        if (isSameUserById(player, authenticatedUser) || isSameUserByName(player, authenticatedUser)) {
            return;
        }

        if (!hasResolvableCurrentPlayerIdentity(player)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the targeted player can " + actionDescription + ".");
    }

    public Game rollDice(Long gameId, String playerToken, RollDiceRequestDTO request) {
        User authenticatedUser = authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));
        ensureBankInitialized(game);

        Player currentPlayer = getCurrentPlayer(game);
        if (currentPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No current player found.");
        }

        ensureCurrentPlayerCanRollDice(currentPlayer, authenticatedUser);
        String currentPhase = game.getTurnPhase();
        
        // Discard call
        if (request != null && request.getDiscardResources() != null) {
            if (!TurnPhase.DISCARD.toString().equals(currentPhase)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Current phase is: " + currentPhase + ". Must be in DISCARD phase.");
            }
            applySevenRollEffects(game, currentPlayer, request.getDiscardResources());
            game.setTurnPhase(TurnPhase.ACTION);
            return gameRepository.save(game);
        }

        if (!TurnPhase.ROLL_DICE.toString().equals(currentPhase)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot roll dice. Current phase is: " + currentPhase + ". Must be in ROLL_DICE phase.");
        }

        int die1 = 1 + (int) (Math.random() * 6);
        int die2 = 1 + (int) (Math.random() * 6);
        int diceSum = die1 + die2;

        game.setDiceValue(diceSum);
        game.setDiceRolledAt(java.time.Instant.now());

        if (diceSum == 7) {
            applySevenRollEffects(game, currentPlayer, null);

            if (totalResourceCards(currentPlayer) > 7) {
                game.setTurnPhase(TurnPhase.DISCARD);
            } else {
                game.setTurnPhase(TurnPhase.ACTION);
            }
            return gameRepository.save(game);
            
        } else {
            distributeResourcesForDiceValue(game, diceSum);
        }
        game.setTurnPhase(TurnPhase.ACTION);

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game rollDice(Long gameId, String playerToken) {
        return rollDice(gameId, playerToken, null);
    }

    void distributeResourcesForDiceValue(Game game, int diceValue) {
        if (game == null) {
            return;
        }

        Board board = game.getBoard();
        if (board == null || board.getIntersections() == null || board.getIntersections().isEmpty()) {
            return;
        }

        List<String> hexTiles = board.getHexTiles();
        List<Integer> hexDiceNumbers = board.getHexTile_DiceNumbers();
        if (hexTiles == null || hexDiceNumbers == null || hexTiles.isEmpty() || hexDiceNumbers.isEmpty()) {
            return;
        }

        int tileCount = Math.min(hexTiles.size(), hexDiceNumbers.size());
        if (tileCount == 0) {
            return;
        }

        Map<Integer, List<Integer>> intersectionToHexIds = board.buildIntersectionToHexIdsMap();

        for (Intersection intersection : board.getIntersections()) {
            if (intersection == null || intersection.getBuilding() == null) {
                continue;
            }

            Building building = intersection.getBuilding();
            Long ownerPlayerId = building.getOwnerPlayerId();
            Integer intersectionId = intersection.getId();
            if (ownerPlayerId == null || intersectionId == null) {
                continue;
            }

            Player owner = findPlayerById(game.getPlayers(), ownerPlayerId);
            if (owner == null) {
                continue;
            }

            int multiplier = resourceMultiplierForBuilding(building);
            if (multiplier < 1) {
                continue;
            }

            List<Integer> adjacentHexIds = intersectionToHexIds.getOrDefault(intersectionId, Collections.emptyList());
            for (Integer adjacentHexId : adjacentHexIds) {
                if (adjacentHexId == null || adjacentHexId < 1 || adjacentHexId > tileCount) {
                    continue;
                }

                // Skip resource production if robber is on this hex
                if (adjacentHexId.equals(game.getRobberTileIndex())) {
                    continue;
                }

                int tileIndex = adjacentHexId - 1;
                Integer tileDiceNumber = hexDiceNumbers.get(tileIndex);
                if (tileDiceNumber == null || tileDiceNumber != diceValue) {
                    continue;
                }

                String tileType = hexTiles.get(tileIndex);
                grantResourceForTile(game, owner, tileType, multiplier);
            }
        }
    }

    void applySevenRollEffects(Game game, Player currentPlayer, Map<String, Integer> discardChoices) {
        if (game == null || game.getPlayers() == null || game.getPlayers().isEmpty()) {
            return;
        }
        ensureBankInitialized(game);

        for (Player player : game.getPlayers()) {
            if (player == null) {
                continue;
            }

            int totalResources = totalResourceCards(player);
            if (totalResources <= 7) {
                continue;
            }

            int resourcesToDiscard = totalResources / 2;

            // If this is the current player, use provided discard choices
            if (currentPlayer != null && player.getId().equals(currentPlayer.getId())) {
                if (discardChoices != null && !discardChoices.isEmpty()) {
                    discardResourcesByChoice(game, player, discardChoices);
                }
                // else: frontend input
            } else {
                // For other players
                discardResourcesRandomly(game, player, resourcesToDiscard);
            }
        }
    }

    void applySevenRollEffects(Game game) {
        applySevenRollEffects(game, null, null);
    }

    public Game buyDevelopmentCard(Long gameId, String playerToken, Long playerId) {
        authenticate(playerToken);

        if (playerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player id is required.");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has no players.");
        }
        ensureBankInitialized(game);

        Player player = findPlayerById(players, playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player with id " + playerId + " was not found.");
        }

        int wool = resourceValue(player.getWool());
        int wheat = resourceValue(player.getWheat());
        int ore = resourceValue(player.getOre());
        if (wool < 1 || wheat < 1 || ore < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Buying a development card costs 1 wool, 1 wheat and 1 ore.");
        }

        String drawnCard = drawDevelopmentCard(game);

        List<String> cards = new ArrayList<>(Optional.ofNullable(player.getDevelopmentCards()).orElse(Collections.emptyList()));
        cards.add(drawnCard);
        player.setDevelopmentCards(cards);

        player.setWool(wool - 1);
        player.setWheat(wheat - 1);
        player.setOre(ore - 1);
        addToBank(game, "wool", 1);
        addToBank(game, "wheat", 1);
        addToBank(game, "ore", 1);

        if (CARD_VICTORY_POINT.equals(drawnCard)) {
            player.setDevelopmentCardVictoryPoints(safeInt(player.getDevelopmentCardVictoryPoints(), 0) + 1);
        }

        game.setPlayers(players);
        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game playKnightCard(Long gameId, String playerToken, Long playerId, Integer targetHexId, Long targetPlayerId) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        Player player = requirePlayer(game, playerId);
        removeDevelopmentCardFromHand(player, CARD_KNIGHT);

        player.setKnightsPlayed(safeInt(player.getKnightsPlayed(), 0) + 1);

        if (targetHexId != null) {
            if (!isValidHexId(game, targetHexId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Hex tile with id " + targetHexId + " was not found.");
            }
            if (targetHexId.equals(game.getRobberTileIndex())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Robber is already on this tile.");
            }
            game.setRobberTileIndex(targetHexId);
        }

        if (targetPlayerId != null && !targetPlayerId.equals(playerId)) {
            Player target = requirePlayer(game, targetPlayerId);
            Integer effectiveRobberHexId = targetHexId != null ? targetHexId : game.getRobberTileIndex();

            if (effectiveRobberHexId == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Robber position is required to steal from a target player.");
            }

            if (!canStealFromPlayer(game, effectiveRobberHexId, targetPlayerId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Target player has no settlements or cities adjacent to robber position on hex " + effectiveRobberHexId + ".");
            }

            stealRandomResource(target, player);
        }

        updateLargestArmyOwnership(game);
        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game playRoadBuildingCard(Long gameId, String playerToken, Long playerId) {
        authenticate(playerToken);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        Player player = requirePlayer(game, playerId);
        removeDevelopmentCardFromHand(player, CARD_ROAD_BUILDING);
        player.setFreeRoadBuildsRemaining(safeInt(player.getFreeRoadBuildsRemaining(), 0) + 2);

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game playYearOfPlentyCard(Long gameId, String playerToken, Long playerId, String resourceA, String resourceB) {
        authenticate(playerToken);

        if (resourceA == null || resourceB == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Year of Plenty requires two resources.");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));
        ensureBankInitialized(game);

        Player player = requirePlayer(game, playerId);
        removeDevelopmentCardFromHand(player, CARD_YEAR_OF_PLENTY);

        if (getBankResourceByName(game, resourceA) < 1 || getBankResourceByName(game, resourceB) < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Bank does not have enough resources for Year of Plenty.");
        }

        setResourceByName(player, resourceA, getResourceByName(player, resourceA) + 1);
        setResourceByName(player, resourceB, getResourceByName(player, resourceB) + 1);
        removeFromBank(game, resourceA, 1);
        removeFromBank(game, resourceB, 1);

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game playMonopolyCard(Long gameId, String playerToken, Long playerId, String resource) {
        authenticate(playerToken);

        if (resource == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Monopoly requires a resource type.");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        List<Player> players = game.getPlayers();
        Player source = requirePlayer(game, playerId);
        removeDevelopmentCardFromHand(source, CARD_MONOPOLY);

        int totalCollected = 0;
        if (players != null) {
            for (Player player : players) {
                if (player == null || player.getId() == null || player.getId().equals(playerId)) {
                    continue;
                }

                int amount = getResourceByName(player, resource);
                if (amount > 0) {
                    setResourceByName(player, resource, 0);
                    totalCollected += amount;
                }
            }
        }

        setResourceByName(source, resource, getResourceByName(source, resource) + totalCollected);

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

        if (game.isSetupPhase()) {
            int settlements = countPlayerSettlements(game, currentPlayer.getId());
            int roads = countPlayerRoads(game, currentPlayer.getId());

            boolean round1Done = game.isFirstSetupRound() && settlements >= 1 && roads >= 1;
            boolean round2Done = game.isSecondSetupRound() && settlements >= 2 && roads >= 2;

            if (!round1Done && !round2Done) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "You must place a settlement and a road before ending your turn.");
            }
            advanceSetupTurn(game);
        } else {
            Integer nextTurnIndex = (game.getCurrentTurnIndex() + 1) % players.size();
            game.setCurrentTurnIndex(nextTurnIndex);
            game.setTurnPhase(TurnPhase.ROLL_DICE.toString());
            game.setDiceValue(null);
        }

        recalculateVictoryState(game);
        return gameRepository.save(game);
    }

    public Game placeInitialSettlement(Long gameId, String token, Long playerId, Integer intersectionId) {
        authenticate(token);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));
        
        if (!game.isSetupPhase()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not in setup phase.");
        }

        Player currentPlayer = getCurrentPlayer(game);
        if (currentPlayer == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No current player found.");
        }
        if (!currentPlayer.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn.");
        }

        int settlements = countPlayerSettlements(game, playerId);

        if (game.isFirstSetupRound() && settlements >= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already placed first settlement.");
        }
        if (game.isSecondSetupRound() && settlements >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already placed second settlement.");
        }

        Intersection intersection = findIntersectionById(game, intersectionId);
        if (intersection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Intersection with id " + intersectionId + " was not found.");
        }

        if (intersection.isOccupied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Intersection occupied.");
        }
        if (hasAdjacentBuilding(game, intersectionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Too close to another building.");
        }

        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(playerId);
        settlement.setIntersectionId(intersectionId);
        intersection.setBuilding(settlement);

        currentPlayer.setSettlementPoints(
            safeInt(currentPlayer.getSettlementPoints(), 0) + 1
        );
        currentPlayer.setLastPlacedSetupSettlementIntersectionId(intersectionId);
        return gameRepository.save(game);
    }

    public Game placeInitialRoad(Long gameId, String token, Long playerId, Integer edgeId) {
        authenticate(token);

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Game with id " + gameId + " was not found."));

        if (!game.isSetupPhase()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not in setup phase.");
        }

        Player currentPlayer = getCurrentPlayer(game);
        if (currentPlayer == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No current player found.");
        }
        if (!currentPlayer.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn.");
        }
        
        Integer settlementId = currentPlayer.getLastPlacedSetupSettlementIntersectionId();
        if (settlementId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "You must place a settlement before placing a road.");
        }
        
        boolean isSecondSetupRound = game.isSecondSetupRound();
        Integer secondSetupSettlementIntersectionId = isSecondSetupRound
            ? findUnconnectedSetupSettlementIntersection(game, playerId)
            : null;

        int roads = countPlayerRoads(game, playerId);

        if (game.isFirstSetupRound() && roads >= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Already placed road in first setup round.");
        }

        if (game.isSecondSetupRound() && roads >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Already placed road in second setup round.");
        }

        Edge edge = findEdgeById(game, edgeId);
        if (edge == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Edge with id " + edgeId + " was not found.");
        }
        if (edge.isOccupied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Edge occupied.");
        }

        boolean connectedToOwnSettlement =
            hasOwnBuildingAtIntersection(game, edge.getIntersectionAId(), playerId) ||
            hasOwnBuildingAtIntersection(game, edge.getIntersectionBId(), playerId);
        if (!connectedToOwnSettlement) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Road must connect to your own settlement.");
        }

        boolean connectedToNewSettlement =
            edge.getIntersectionAId().equals(settlementId) ||
            edge.getIntersectionBId().equals(settlementId);
        if (!connectedToNewSettlement) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Road must connect to your newly placed settlement.");
        }

        Road road = new Road();
        road.setOwnerPlayerId(playerId);
        road.setEdgeId(edgeId);
        edge.setRoad(road);

        if (isSecondSetupRound && secondSetupSettlementIntersectionId != null) {
            grantInitialSettlementResources(game, currentPlayer, secondSetupSettlementIntersectionId);
        }

        currentPlayer.setLastPlacedSetupSettlementIntersectionId(null);
        return gameRepository.save(game);
    }

    private void advanceSetupTurn(Game game) {
        List<Player> players = game.getPlayers();
        int current = game.getCurrentTurnIndex();

        if (game.isFirstSetupRound()) {
            int next = current + 1;
            if (next >= players.size()) {
                game.setGamePhase("SETUP_SECOND_ROUND");
                game.setCurrentTurnIndex(players.size() - 1);
            } else {
                game.setCurrentTurnIndex(next);
            }
        }
        else if (game.isSecondSetupRound()) {
            int next = current - 1;
            if (next < 0) {
                game.setGamePhase("ACTIVE");
                game.setCurrentTurnIndex(0);
                game.setTurnPhase(TurnPhase.ROLL_DICE.toString());
            } else {
                game.setCurrentTurnIndex(next);
            }
        }
    }

    private int countPlayerSettlements(Game game, Long playerId) {
        if (game == null || game.getBoard() == null || game.getBoard().getIntersections() == null) {
            return 0;
        }
        return (int) game.getBoard().getIntersections().stream()
            .filter(i -> i.getBuilding() instanceof Settlement)
            .map(i -> (Settlement) i.getBuilding())
            .filter(s -> playerId.equals(s.getOwnerPlayerId()))
            .count();
    }

    private int countPlayerRoads(Game game, Long playerId) {
        if (game == null || game.getBoard() == null || game.getBoard().getEdges() == null) {
            return 0;
        }
        return (int) game.getBoard().getEdges().stream()
            .filter(e -> e.getRoad() != null)
            .filter(e -> playerId.equals(e.getRoad().getOwnerPlayerId()))
            .count();
    }

    private Integer findUnconnectedSetupSettlementIntersection(Game game, Long playerId) {
        if (game == null || playerId == null || game.getBoard() == null || game.getBoard().getIntersections() == null) {
            return null;
        }

        for (Intersection intersection : game.getBoard().getIntersections()) {
            if (intersection == null || !(intersection.getBuilding() instanceof Settlement)) {
                continue;
            }

            Settlement settlement = (Settlement) intersection.getBuilding();
            if (!playerId.equals(settlement.getOwnerPlayerId())) {
                continue;
            }

            if (!hasOwnRoadAtIntersection(game, intersection.getId(), playerId)) {
                return intersection.getId();
            }
        }

        return null;
    }

    private void grantInitialSettlementResources(Game game, Player player, Integer intersectionId) {
        if (game == null || player == null || game.getBoard() == null || game.getBoard().getHexTiles() == null) {
            return;
        }
        ensureBankInitialized(game);

        List<Integer> adjacentHexIds = game.getBoard().getAdjacentHexIdsForIntersection(intersectionId);
        for (Integer hexId : adjacentHexIds) {
            String tileType = getHexTileType(game, hexId);
            if (tileType == null || "DESERT".equalsIgnoreCase(tileType)) {
                continue;
            }

            // Test resources - REMOVE LATER
            //player.setWood(resourceValue(player.getWood()) + 3);
            //player.setBrick(resourceValue(player.getBrick()) + 3);
            //player.setWool(resourceValue(player.getWool()) + 3);
            //player.setWheat(resourceValue(player.getWheat()) + 3);
            //player.setOre(resourceValue(player.getOre()) + 3);

            switch (tileType.toUpperCase()) {
                case "WOOD" -> player.setWood(resourceValue(player.getWood()) + 1);
                case "BRICK" -> player.setBrick(resourceValue(player.getBrick()) + 1);
                case "SHEEP", "WOOL" -> player.setWool(resourceValue(player.getWool()) + 1);
                case "WHEAT" -> player.setWheat(resourceValue(player.getWheat()) + 1);
                case "ORE" -> player.setOre(resourceValue(player.getOre()) + 1);
                default -> {
                    // Ignore unknown tile labels.
                }
            }
            grantResourceForTile(game, player, tileType, 1);
        }
    }

    private String getHexTileType(Game game, Integer hexId) {
        if (game == null || game.getBoard() == null || game.getBoard().getHexTiles() == null || hexId == null) {
            return null;
        }

        int index = hexId - 1;
        if (index < 0 || index >= game.getBoard().getHexTiles().size()) {
            return null;
        }

        return game.getBoard().getHexTiles().get(index);
    }

    private boolean isValidHexId(Game game, Integer hexId) {
        if (game == null || hexId == null || game.getBoard() == null || game.getBoard().getHexTiles() == null) {
            return false;
        }

        int index = hexId - 1;
        return index >= 0 && index < game.getBoard().getHexTiles().size();
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
            Integer robberTileIndex = gamePostDTO.getRobberTileIndex();
            if (board == null || board.getHexTiles() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Hex tile with id " + robberTileIndex + " was not found.");
            }
            int index = robberTileIndex - 1;
            if (index < 0 || index >= board.getHexTiles().size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Hex tile with id " + robberTileIndex + " was not found.");
            }
            return robberTileIndex;
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
            return new ArrayList<>();
        }

        return players.stream()
                .map(this::convertPlayerDtoToEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Player convertPlayerDtoToEntity(PlayerGetDTO playerDto) {
        if (playerDto == null) {
            return null;
        }

        Player player = new Player();
        player.setId(playerDto.getId());
        player.setName(playerDto.getName());

        // Fetch the managed User entity when available, otherwise attach a lightweight
        // fallback User so player identity checks/tests never dereference null.
        if (playerDto.getId() != null) {
            try {
                User existingUser = userService.getUserById(playerDto.getId());
                if (existingUser != null) {
                    player.setUser(existingUser);
                } else {
                    User fallbackUser = new User();
                    fallbackUser.setId(playerDto.getId());
                    fallbackUser.setUsername(playerDto.getName());
                    player.setUser(fallbackUser);
                }
            } catch (ResponseStatusException e) {
                User fallbackUser = new User();
                fallbackUser.setId(playerDto.getId());
                fallbackUser.setUsername(playerDto.getName());
                player.setUser(fallbackUser);
            }
        } else {
            User fallbackUser = new User();
            fallbackUser.setUsername(playerDto.getName());
            player.setUser(fallbackUser);
        }

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
        player.setDevelopmentCards(playerDto.getDevelopmentCards());
        player.setKnightsPlayed(playerDto.getKnightsPlayed());
        player.setFreeRoadBuildsRemaining(playerDto.getFreeRoadBuildsRemaining());
        player.recalculateVictoryPoints();
        return player;
    }

    private Player requirePlayer(Game game, Long playerId) {
        if (playerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player id is required.");
        }

        Player player = findPlayerById(game.getPlayers(), playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player with id " + playerId + " was not found.");
        }
        return player;
    }

    private void removeDevelopmentCardFromHand(Player player, String cardName) {
        List<String> hand = new ArrayList<>(Optional.ofNullable(player.getDevelopmentCards()).orElse(Collections.emptyList()));
        int index = hand.indexOf(cardName);
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Player does not own development card: " + cardName + ".");
        }

        hand.remove(index);
        player.setDevelopmentCards(hand);
    }

    private void stealRandomResource(Player from, Player to) {
        List<String> available = new ArrayList<>();
        addResourceIfAvailable(available, "wood", from.getWood());
        addResourceIfAvailable(available, "brick", from.getBrick());
        addResourceIfAvailable(available, "wool", from.getWool());
        addResourceIfAvailable(available, "wheat", from.getWheat());
        addResourceIfAvailable(available, "ore", from.getOre());

        if (available.isEmpty()) {
            return;
        }

        String selected = available.get(ThreadLocalRandom.current().nextInt(available.size()));
        setResourceByName(from, selected, getResourceByName(from, selected) - 1);
        setResourceByName(to, selected, getResourceByName(to, selected) + 1);
    }

    private void addResourceIfAvailable(List<String> available, String resource, Integer amount) {
        int value = resourceValue(amount);
        for (int i = 0; i < value; i++) {
            available.add(resource);
        }
    }

    private void updateLargestArmyOwnership(Game game) {
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            return;
        }

        Player currentHolder = players.stream()
            .filter(player -> player != null && Boolean.TRUE.equals(player.getHasLargestArmy()))
            .findFirst()
            .orElse(null);

        int highestKnightCount = players.stream()
            .filter(Objects::nonNull)
            .mapToInt(player -> safeInt(player.getKnightsPlayed(), 0))
            .max()
            .orElse(0);

        if (highestKnightCount < 3) {
            players.forEach(player -> {
                if (player != null) {
                    player.setHasLargestArmy(false);
                }
            });
            return;
        }

        List<Player> leaders = players.stream()
            .filter(player -> player != null && safeInt(player.getKnightsPlayed(), 0) == highestKnightCount)
            .toList();

        Player holder = null;
        if (currentHolder != null && safeInt(currentHolder.getKnightsPlayed(), 0) == highestKnightCount) {
            holder = currentHolder;
        }
        else if (leaders.size() == 1) {
            holder = leaders.get(0);
        }

        final Long holderId = holder == null ? null : holder.getId();
        players.forEach(player -> {
            if (player != null) {
                player.setHasLargestArmy(holderId != null && holderId.equals(player.getId()));
            }
        });
    }

    private void initializeDevelopmentDeck(Game game, ch.uzh.ifi.hase.soprafs26.rest.dto.DevelopmentDeckGetDTO deckDto) {
        if (deckDto == null || isBlankDevelopmentDeck(deckDto)) {
            game.setDevelopmentKnightRemaining(14);
            game.setDevelopmentVictoryPointRemaining(5);
            game.setDevelopmentRoadBuildingRemaining(2);
            game.setDevelopmentYearOfPlentyRemaining(2);
            game.setDevelopmentMonopolyRemaining(2);
            return;
        }

        game.setDevelopmentKnightRemaining(Math.max(0, safeInt(deckDto.getKnight(), 14)));
        game.setDevelopmentVictoryPointRemaining(Math.max(0, safeInt(deckDto.getVictoryPoint(), 5)));
        game.setDevelopmentRoadBuildingRemaining(Math.max(0, safeInt(deckDto.getRoadBuilding(), 2)));
        game.setDevelopmentYearOfPlentyRemaining(Math.max(0, safeInt(deckDto.getYearOfPlenty(), 2)));
        game.setDevelopmentMonopolyRemaining(Math.max(0, safeInt(deckDto.getMonopoly(), 2)));
    }

    private boolean isBlankDevelopmentDeck(ch.uzh.ifi.hase.soprafs26.rest.dto.DevelopmentDeckGetDTO deckDto) {
        return safeInt(deckDto.getKnight(), 0) <= 0
            && safeInt(deckDto.getVictoryPoint(), 0) <= 0
            && safeInt(deckDto.getRoadBuilding(), 0) <= 0
            && safeInt(deckDto.getYearOfPlenty(), 0) <= 0
            && safeInt(deckDto.getMonopoly(), 0) <= 0;
    }

    private String drawDevelopmentCard(Game game) {
        int knight = safeInt(game.getDevelopmentKnightRemaining(), 0);
        int victoryPoint = safeInt(game.getDevelopmentVictoryPointRemaining(), 0);
        int roadBuilding = safeInt(game.getDevelopmentRoadBuildingRemaining(), 0);
        int yearOfPlenty = safeInt(game.getDevelopmentYearOfPlentyRemaining(), 0);
        int monopoly = safeInt(game.getDevelopmentMonopolyRemaining(), 0);

        int total = knight + victoryPoint + roadBuilding + yearOfPlenty + monopoly;
        if (total <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No development cards left in the deck.");
        }

        int draw = ThreadLocalRandom.current().nextInt(total);
        if (draw < knight) {
            game.setDevelopmentKnightRemaining(knight - 1);
            return CARD_KNIGHT;
        }

        draw -= knight;
        if (draw < victoryPoint) {
            game.setDevelopmentVictoryPointRemaining(victoryPoint - 1);
            return CARD_VICTORY_POINT;
        }

        draw -= victoryPoint;
        if (draw < roadBuilding) {
            game.setDevelopmentRoadBuildingRemaining(roadBuilding - 1);
            return CARD_ROAD_BUILDING;
        }

        draw -= roadBuilding;
        if (draw < yearOfPlenty) {
            game.setDevelopmentYearOfPlentyRemaining(yearOfPlenty - 1);
            return CARD_YEAR_OF_PLENTY;
        }

        game.setDevelopmentMonopolyRemaining(monopoly - 1);
        return CARD_MONOPOLY;
    }

    private void recalculateVictoryState(Game game) {
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            game.setWinner(null);
            game.setFinishedAt(null);
            return;
        }

        updateLongestRoadOwnership(game);
        updateLargestArmyOwnership(game);

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
        return userService.authenticate(playerToken);
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

    private int totalResourceCards(Player player) {
        if (player == null) {
            return 0;
        }

        return resourceValue(player.getWood())
            + resourceValue(player.getBrick())
            + resourceValue(player.getWool())
            + resourceValue(player.getWheat())
            + resourceValue(player.getOre());
    }

    private void discardResourcesByChoice(Game game, Player player, Map<String, Integer> discardChoices) {
        if (player == null || discardChoices == null || discardChoices.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> entry : discardChoices.entrySet()) {
            String resource = entry.getKey();
            Integer amount = entry.getValue();
            if (amount != null && amount > 0) {
                int available = getResourceByName(player, resource);
                int toDiscard = Math.min(amount, available);
                setResourceByName(player, resource, available - toDiscard);
                addToBank(game, resource, toDiscard);
            }
        }
    }

    private void discardResourcesRandomly(Game game, Player player, int toDiscard) {
        List<String> pool = new ArrayList<>();

        addResource(pool, "wood", player.getWood());
        addResource(pool, "brick", player.getBrick());
        addResource(pool, "wool", player.getWool());
        addResource(pool, "wheat", player.getWheat());
        addResource(pool, "ore", player.getOre());

        Collections.shuffle(pool);

        for (int i = 0; i < toDiscard && i < pool.size(); i++) {
            String resource = pool.get(i);
            int current = getResourceByName(player, resource);
            setResourceByName(player, resource, current - 1);
            addToBank(game, resource, 1);
        }
    }

    private void addResource(List<String> pool, String resource, Integer amount) {
        if (pool == null || resource == null || amount == null || amount <= 0) {
            return;
        }

        for (int i = 0; i < amount; i++) {
            pool.add(resource);
        }
    }

    private int resourceMultiplierForBuilding(Building building) {
        if (building instanceof City) {
            return 2;
        }

        if (building instanceof Settlement) {
            return 1;
        }

        return 0;
    }

    private void grantResourceForTile(Game game, Player player, String tileType, int amount) {
        if (game == null || player == null || tileType == null || amount < 1) {
            return;
        }

        String normalized = tileType.trim().toUpperCase(Locale.ROOT);
        String resourceName = switch (normalized) {
            case "WOOD" -> "wood";
            case "BRICK" -> "brick";
            case "WHEAT" -> "wheat";
            case "SHEEP", "WOOL" -> "wool";
            case "ORE" -> "ore";
            default -> null;
        };

        if (resourceName == null) {
            return;
        }

        int availableInBank = getBankResourceByName(game, resourceName);
        int delivered = Math.min(availableInBank, amount);
        if (delivered <= 0) {
            return;
        }

        removeFromBank(game, resourceName, delivered);
        setResourceByName(player, resourceName, getResourceByName(player, resourceName) + delivered);
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

    private void initializeBankResources(Game game, Map<String, Integer> bankResources) {
        if (bankResources == null) {
            game.setBankWood(BANK_STARTING_RESOURCE_COUNT);
            game.setBankBrick(BANK_STARTING_RESOURCE_COUNT);
            game.setBankWool(BANK_STARTING_RESOURCE_COUNT);
            game.setBankWheat(BANK_STARTING_RESOURCE_COUNT);
            game.setBankOre(BANK_STARTING_RESOURCE_COUNT);
            return;
        }

        applyBankResourcesFromMap(game, bankResources);
    }

    private void applyBankResourcesFromMap(Game game, Map<String, Integer> bankResources) {
        game.setBankWood(Math.max(0, Optional.ofNullable(bankResources.get("wood")).orElse(0)));
        game.setBankBrick(Math.max(0, Optional.ofNullable(bankResources.get("brick")).orElse(0)));
        game.setBankWool(Math.max(0, Optional.ofNullable(bankResources.get("wool")).orElse(0)));
        game.setBankWheat(Math.max(0, Optional.ofNullable(bankResources.get("wheat")).orElse(0)));
        game.setBankOre(Math.max(0, Optional.ofNullable(bankResources.get("ore")).orElse(0)));
    }

    private void ensureBankInitialized(Game game) {
        if (game == null) {
            return;
        }

        if (game.getBankWood() != null
            && game.getBankBrick() != null
            && game.getBankWool() != null
            && game.getBankWheat() != null
            && game.getBankOre() != null) {
            return;
        }

        List<Player> players = Optional.ofNullable(game.getPlayers()).orElse(Collections.emptyList());
        int playersWood = players.stream().filter(Objects::nonNull).mapToInt(player -> resourceValue(player.getWood())).sum();
        int playersBrick = players.stream().filter(Objects::nonNull).mapToInt(player -> resourceValue(player.getBrick())).sum();
        int playersWool = players.stream().filter(Objects::nonNull).mapToInt(player -> resourceValue(player.getWool())).sum();
        int playersWheat = players.stream().filter(Objects::nonNull).mapToInt(player -> resourceValue(player.getWheat())).sum();
        int playersOre = players.stream().filter(Objects::nonNull).mapToInt(player -> resourceValue(player.getOre())).sum();

        game.setBankWood(Math.max(0, BANK_STARTING_RESOURCE_COUNT - playersWood));
        game.setBankBrick(Math.max(0, BANK_STARTING_RESOURCE_COUNT - playersBrick));
        game.setBankWool(Math.max(0, BANK_STARTING_RESOURCE_COUNT - playersWool));
        game.setBankWheat(Math.max(0, BANK_STARTING_RESOURCE_COUNT - playersWheat));
        game.setBankOre(Math.max(0, BANK_STARTING_RESOURCE_COUNT - playersOre));
    }

    private int getBankResourceByName(Game game, String resource) {
        if (game == null) {
            return 0;
        }

        return switch (normalizeResourceName(resource)) {
            case "wood" -> resourceValue(game.getBankWood());
            case "brick" -> resourceValue(game.getBankBrick());
            case "wool" -> resourceValue(game.getBankWool());
            case "wheat" -> resourceValue(game.getBankWheat());
            case "ore" -> resourceValue(game.getBankOre());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported resource: " + resource);
        };
    }

    private void setBankResourceByName(Game game, String resource, int value) {
        int normalized = Math.max(0, value);
        switch (normalizeResourceName(resource)) {
            case "wood" -> game.setBankWood(normalized);
            case "brick" -> game.setBankBrick(normalized);
            case "wool" -> game.setBankWool(normalized);
            case "wheat" -> game.setBankWheat(normalized);
            case "ore" -> game.setBankOre(normalized);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported resource: " + resource);
        }
    }

    private void addToBank(Game game, String resource, int amount) {
        if (game == null || amount <= 0) {
            return;
        }

        int current = getBankResourceByName(game, resource);
        setBankResourceByName(game, resource, current + amount);
    }

    private void removeFromBank(Game game, String resource, int amount) {
        if (game == null || amount <= 0) {
            return;
        }

        int current = getBankResourceByName(game, resource);
        setBankResourceByName(game, resource, Math.max(0, current - amount));
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

    private boolean canStealFromPlayer(Game game, Integer robberHexId, Long targetPlayerId) {
        if (game == null || robberHexId == null || targetPlayerId == null) {
            return false;
        }

        Board board = game.getBoard();
        if (board == null) {
            return false;
        }

        // Use Board's geometry to get all intersection IDs for this hex
        List<Integer> robberHexIntersections = board.getIntersectionIdsForHex(robberHexId);
        if (robberHexIntersections.isEmpty()) {
            return false;
        }

        // Check if target player has a settlement or city on any of the intersections of the robber hex
        for (Integer intersectionId : robberHexIntersections) {
            Intersection intersection = findIntersectionById(game, intersectionId);
            if (intersection != null && intersection.getBuilding() != null) {
                if (targetPlayerId.equals(intersection.getBuilding().getOwnerPlayerId())) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<Integer> getIntersectionsForHex(Integer hexId) {
        List<Integer> intersectionIds = new ArrayList<>();
        if (hexId == null || hexId < 1 || hexId > 19) {
            return intersectionIds;
        }

        // Board constants from entity.Board
        final double HEX_SIZE = 58.0;
        final double SQRT_3 = Math.sqrt(3.0);
        final double ORIGIN_X = 150.0;
        final double ORIGIN_Y = 130.0;
        final double HEX_SPACING_X = HEX_SIZE * SQRT_3;
        final double HEX_SPACING_Y = HEX_SIZE * 1.5;

        // Get hex center coordinates
        double[] boardCoordinates = getBoardCoordinatesForHex(hexId);
        if (boardCoordinates == null) {
            return intersectionIds;
        }

        double centerX = ORIGIN_X + boardCoordinates[0] * HEX_SPACING_X;
        double centerY = ORIGIN_Y + boardCoordinates[1] * HEX_SPACING_Y;

        // Get the 6 corner points for this hex
        for (int cornerIndex = 0; cornerIndex < 6; cornerIndex++) {
            double angle = (Math.PI / 3.0) * cornerIndex + Math.PI / 6.0;
            double cornerX = centerX + HEX_SIZE * Math.cos(angle);
            double cornerY = centerY + HEX_SIZE * Math.sin(angle);

            // Format point key same as Board does
            String cornerKey = Math.round(cornerX) + ":" + Math.round(cornerY);

            // Map corner key to intersection ID
            // This is derived from the Board's createDefaultIntersectionsAndEdges logic
            // We'll iterate through all hexes and find the intersection ID for this corner
            Integer intersectionId = getIntersectionIdForCornerKey(cornerKey);
            if (intersectionId != null) {
                intersectionIds.add(intersectionId);
            }
        }

        return intersectionIds;
    }

    private double[] getBoardCoordinatesForHex(Integer hexId) {
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
            default -> null;
        };
    }

    private Integer getIntersectionIdForCornerKey(String cornerKey) {
        // This approach ensures consistency with Board.createDefaultIntersectionsAndEdges()

        Map<String, Integer> cornerKeyToIntersectionId = new HashMap<>();

        // Board generates intersections by iterating through hexes 1-19
        // For each hex, it gets 6 corners and creates/reuses intersections
        // The corner key is formatPoint(x, y) = roundX + ":" + roundY

        // These values are computed from the board geometry equations:
        // boardCoordinatesForHex(hexId), getCornerPoint(centerX, centerY, cornerIndex)
        // HEX_SIZE = 58.0, SQRT_3 ≈ 1.732, ORIGIN_X = 150, ORIGIN_Y = 130
        // HEX_SPACING_X = 100.33, HEX_SPACING_Y = 87

        // Row 0 (Top): hexes 1-3
        // Hex 1 (1,0): center ~(250, 130)
        // Hex 2 (2,0): center ~(350, 130) 
        // Hex 3 (3,0): center ~(450, 130)
        
        // Top intersections
        cornerKeyToIntersectionId.put("281:104", 0);     // Hex 1 top-right = Hex 2 top-left
        cornerKeyToIntersectionId.put("339:104", 1);     // Hex 2 top-right = Hex 3 top-left
        cornerKeyToIntersectionId.put("397:104", 2);     // Hex 3 top-right
        cornerKeyToIntersectionId.put("455:104", 3);

        cornerKeyToIntersectionId.put("252:145", 4);     // Left side of row 0
        cornerKeyToIntersectionId.put("310:145", 5);
        cornerKeyToIntersectionId.put("368:145", 6);
        cornerKeyToIntersectionId.put("426:145", 7);
        cornerKeyToIntersectionId.put("484:145", 8);

        cornerKeyToIntersectionId.put("223:186", 9);     // Row 1 left
        cornerKeyToIntersectionId.put("281:186", 10);
        cornerKeyToIntersectionId.put("339:186", 11);
        cornerKeyToIntersectionId.put("397:186", 12);
        cornerKeyToIntersectionId.put("455:186", 13);
        cornerKeyToIntersectionId.put("513:186", 14);

        cornerKeyToIntersectionId.put("194:227", 15);    // Between rows 1 and 2
        cornerKeyToIntersectionId.put("252:227", 16);
        cornerKeyToIntersectionId.put("310:227", 17);
        cornerKeyToIntersectionId.put("368:227", 18);
        cornerKeyToIntersectionId.put("426:227", 19);
        cornerKeyToIntersectionId.put("484:227", 20);
        cornerKeyToIntersectionId.put("542:227", 21);

        cornerKeyToIntersectionId.put("165:268", 22);    // Row 2 (middle)
        cornerKeyToIntersectionId.put("223:268", 23);
        cornerKeyToIntersectionId.put("281:268", 24);
        cornerKeyToIntersectionId.put("339:268", 25);
        cornerKeyToIntersectionId.put("397:268", 26);
        cornerKeyToIntersectionId.put("455:268", 27);
        cornerKeyToIntersectionId.put("513:268", 28);
        cornerKeyToIntersectionId.put("571:268", 29);

        cornerKeyToIntersectionId.put("194:309", 30);    // Between rows 2 and 3
        cornerKeyToIntersectionId.put("252:309", 31);
        cornerKeyToIntersectionId.put("310:309", 32);
        cornerKeyToIntersectionId.put("368:309", 33);
        cornerKeyToIntersectionId.put("426:309", 34);
        cornerKeyToIntersectionId.put("484:309", 35);
        cornerKeyToIntersectionId.put("542:309", 36);

        cornerKeyToIntersectionId.put("223:350", 37);    // Row 3
        cornerKeyToIntersectionId.put("281:350", 38);
        cornerKeyToIntersectionId.put("339:350", 39);
        cornerKeyToIntersectionId.put("397:350", 40);
        cornerKeyToIntersectionId.put("455:350", 41);
        cornerKeyToIntersectionId.put("513:350", 42);

        cornerKeyToIntersectionId.put("252:391", 43);    // Between rows 3 and 4
        cornerKeyToIntersectionId.put("310:391", 44);
        cornerKeyToIntersectionId.put("368:391", 45);
        cornerKeyToIntersectionId.put("426:391", 46);
        cornerKeyToIntersectionId.put("484:391", 47);

        cornerKeyToIntersectionId.put("281:432", 48);    // Row 4 (bottom)
        cornerKeyToIntersectionId.put("339:432", 49);
        cornerKeyToIntersectionId.put("397:432", 50);
        cornerKeyToIntersectionId.put("455:432", 51);

        cornerKeyToIntersectionId.put("310:473", 52);    // Bottom
        cornerKeyToIntersectionId.put("368:473", 53);

        return cornerKeyToIntersectionId.get(cornerKey);
    }
}