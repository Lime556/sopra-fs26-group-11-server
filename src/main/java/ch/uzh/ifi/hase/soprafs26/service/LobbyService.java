package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PlayerRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class LobbyService {



    private static final int DEFAULT_LOBBY_CAPACITY = 4;


    private final GameRepository gameRepository;
    private final LobbyRepository lobbyRepository;
    private final PlayerRepository playerRepository;
    private final UserService userService;
    
    public LobbyService(
        LobbyRepository lobbyRepository,
        GameRepository gameRepository,
        PlayerRepository playerRepository,
        UserService userService
    ) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.userService = userService;
                        
    }

    public List<Lobby> getLobbies() {
        return lobbyRepository.findAll();
    }

    public Lobby createLobby(String playerToken, Integer capacity, String lobbyPassword) {
        User host = userService.authenticate(playerToken);

        Lobby lobby = new Lobby();
        lobby.setCapacity(resolveCapacity(capacity));
        lobby.setHostId(host.getId());
        lobby.setGameId(null);

        if (lobbyPassword != null && !lobbyPassword.isBlank()) {
            lobby.setPassword(lobbyPassword.trim());
        }
        lobby.getUsers().add(host);

        Lobby createdLobby = lobbyRepository.save(lobby);
        lobbyRepository.flush();
        return createdLobby;
    }

    public Lobby joinLobby(Long lobbyId, String playerToken, String lobbyPassword) {
        User user = userService.authenticate(playerToken);

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        if (lobby.getPassword() != null && !lobby.getPassword().isBlank()) {
            if (lobbyPassword == null || !lobby.getPassword().equals(lobbyPassword)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Wrong lobby password for lobby " + lobbyId + ".");
            }
        }

        boolean alreadyJoined = lobby.getUsers().stream()
                .anyMatch(existingUser -> existingUser.getId().equals(user.getId()));
        if (alreadyJoined) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User with id " + user.getId() + " already joined lobby " + lobbyId + ".");
        }

        if (lobby.getCurrentPlayers() >= lobby.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lobby with id " + lobbyId + " is full.");
        }

        lobby.getUsers().add(user);
        lobby = lobbyRepository.save(lobby);
        lobbyRepository.flush();
        return lobby;
    }

    public Game startGame (Long lobbyId, String playerToken) {
        User requester = userService.authenticate(playerToken);

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        boolean requesterInLobby = lobby.getUsers().stream()
        .anyMatch(user -> user.getId().equals(requester.getId()));
                
        if (!requesterInLobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this lobby");
        }

        if (!lobby.getHostId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the game");
        }

        if (lobby.getUsers().size() < 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough players");
        }

        if (lobby.getGameId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already started");
        }

        Game game = new Game();
        game.setCurrentTurnIndex(0);
        game.setDiceValue(null);
        game.setTargetVictoryPoints(10);
        game.setStartedAt(java.time.LocalDateTime.now());
        game.setFinishedAt(null);

        game = gameRepository.save(game);

        List<User> orderedUsers = lobby.getUsers().stream()
            .sorted((u1, u2) -> u1.getId().compareTo(u2.getId()))
            .toList();

        String[] colors = {"RED", "BLUE", "WHITE", "ORANGE"};

        for (int i = 0; i < orderedUsers.size(); i++) {
            Player player = new Player();
            player.setUser(orderedUsers.get(i));
            player.setGameId(game.getId());
            player.setColor(colors[i]);
            player.setVictoryPoints(0);
            playerRepository.save(player);
        }

        lobby.setGameId(game.getId());
        lobbyRepository.save(lobby);
        lobbyRepository.flush();

        return game;
    }

    private int resolveCapacity(Integer capacity) {
        int resolved = capacity == null ? DEFAULT_LOBBY_CAPACITY : capacity;
        if (resolved < 2 || resolved > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby capacity must be between 2 and 4.");
        }
        return resolved;
    }

    
}
