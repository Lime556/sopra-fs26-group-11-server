package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
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

    @GetMapping("/games/{gameId}/board")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BoardGetDTO getBoardById(@PathVariable Long gameId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return convertBoardToDto(gameService.getBoardById(gameId, extractToken(authorizationHeader)));
    }

    private GameGetDTO convertGameToDto(Game game) {
        GameGetDTO dto = new GameGetDTO();
        dto.setId(game.getId());
        dto.setBoard(convertBoardToDto(game.getBoard()));
        dto.setCurrentTurnIndex(game.getCurrentTurnIndex());
        dto.setDiceValue(game.getDiceValue());
        dto.setRobberTileIndex(game.getRobberTileIndex());
        dto.setTargetVictoryPoints(game.getTargetVictoryPoints());
        dto.setStartedAt(game.getStartedAt());
        dto.setFinishedAt(game.getFinishedAt());
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
        dto.setHexTile_DiceNumbers(board.getHexTile_DiceNumbers());
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
}