package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.BoatGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerGetDTO;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
        Board board = resolveBoard(gamePostDTO);
        game.setBoard(board);
        game.setPlayers(convertPlayerDtosToEntity(gamePostDTO == null ? null : gamePostDTO.getPlayers()));
        game.setCurrentTurnIndex(gamePostDTO == null ? null : gamePostDTO.getCurrentTurnIndex());
        game.setDiceValue(gamePostDTO == null ? null : gamePostDTO.getDiceValue());
        game.setRobberTileIndex(resolveRobberTileIndex(board, gamePostDTO));
        game.setTargetVictoryPoints(resolveTargetVictoryPoints(gamePostDTO));
        game.setStartedAt(gamePostDTO == null ? null : gamePostDTO.getStartedAt());
        game.setFinishedAt(gamePostDTO == null ? null : gamePostDTO.getFinishedAt());
        game.setWinner(convertPlayerDtoToEntity(gamePostDTO == null ? null : gamePostDTO.getWinner()));

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

            game.setPlayers(convertPlayerDtosToEntity(gamePostDTO.getPlayers()));
            game.setCurrentTurnIndex(gamePostDTO.getCurrentTurnIndex());
            game.setDiceValue(gamePostDTO.getDiceValue());

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
        player.setVictoryPoints(playerDto.getVictoryPoints());
        player.setSettlementPoints(playerDto.getSettlementPoints());
        player.setCityPoints(playerDto.getCityPoints());
        player.setDevelopmentCardVictoryPoints(playerDto.getDevelopmentCardVictoryPoints());
        player.setHasLongestRoad(playerDto.getHasLongestRoad());
        player.setHasLargestArmy(playerDto.getHasLargestArmy());
        return player;
    }

    private void recalculateVictoryState(Game game) {
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            game.setWinner(null);
            game.setFinishedAt(null);
            return;
        }

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

        if (topPlayer != null && topPlayer.getVictoryPoints() != null && topPlayer.getVictoryPoints() >= targetVictoryPoints) {
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
}