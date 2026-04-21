package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.*;
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

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoatGetDTO;
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
            game.getTurnPhase(),
            game.getDiceValue(),
            currentPlayer != null ? currentPlayer.getId() : null,
            currentPlayer != null ? currentPlayer.getName() : null,
            game.getFinishedAt() != null && game.getWinner() != null
        );
        messaging.convertAndSend(String.format("/topic/games/%d/state", gameId), stateDTO);
        return stateDTO;
    }

    private GameGetDTO convertGameToDto(Game game) {
        GameGetDTO dto = new GameGetDTO();
        dto.setId(game.getId());
        dto.setBoard(convertBoardToDto(game.getBoard()));
        dto.setCurrentTurnIndex(game.getCurrentTurnIndex());
        dto.setTurnPhase(game.getTurnPhase());
        dto.setDiceValue(game.getDiceValue());
        dto.setRobberTileIndex(game.getRobberTileIndex());
        dto.setTargetVictoryPoints(game.getTargetVictoryPoints());
        dto.setStartedAt(game.getStartedAt());
        dto.setFinishedAt(game.getFinishedAt());
        dto.setPlayers(convertPlayersToDto(game.getPlayers()));
        dto.setWinner(convertPlayerToDto(game.getWinner()));
        dto.setGameFinished(game.getFinishedAt() != null && game.getWinner() != null);
        dto.setChatMessages(game.getChatMessages());
        return dto;
    }

    private List<PlayerGetDTO> convertPlayersToDto(List<Player> players) {
        if (players == null) {
            return Collections.emptyList();
        }

        return players.stream().map(this::convertPlayerToDto).collect(Collectors.toList());
    }

    private PlayerGetDTO convertPlayerToDto(Player player) {
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
    public DiceRollDTO getDiceRoll(@PathVariable Long gameId) {
        Game game = gameService.getGameById(gameId, null);
        DiceRollDTO dto = new DiceRollDTO();
        dto.setDiceValue(game.getDiceValue());
        dto.setDiceRolledAt(game.getDiceRolledAt());
        return dto;
    }
}