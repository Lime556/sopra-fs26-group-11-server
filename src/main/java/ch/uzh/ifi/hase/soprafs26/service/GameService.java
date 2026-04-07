package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        game.setBoard(resolveBoard(gamePostDTO));
        game.setCurrentTurnIndex(gamePostDTO == null ? null : gamePostDTO.getCurrentTurnIndex());
        game.setDiceValue(gamePostDTO == null ? null : gamePostDTO.getDiceValue());
        game.setTargetVictoryPoints(gamePostDTO == null ? null : gamePostDTO.getTargetVictoryPoints());
        game.setStartedAt(gamePostDTO == null ? null : gamePostDTO.getStartedAt());
        game.setFinishedAt(gamePostDTO == null ? null : gamePostDTO.getFinishedAt());

        return gameRepository.save(game);
    }

    public Game getGameById(Long gameId, String playerToken) {
        authenticate(playerToken);

        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Game with id " + gameId + " was not found."));
    }

    public Board getBoardById(Long gameId, String playerToken) {
        return getGameById(gameId, playerToken).getBoard();
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
        board.setHexTile_DiceNumbers(boardGetDTO.getHexTile_DiceNumbers());
        return board;
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
}