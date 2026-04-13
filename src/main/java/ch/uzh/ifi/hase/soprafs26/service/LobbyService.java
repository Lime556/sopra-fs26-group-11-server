package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyParticipantRepository;
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
    private final LobbyParticipantRepository lobbyParticipantRepository;
    
    public LobbyService(
        LobbyRepository lobbyRepository,
        GameRepository gameRepository,
        PlayerRepository playerRepository,
        LobbyParticipantRepository lobbyParticipantRepository,
        UserService userService
    ) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.lobbyParticipantRepository = lobbyParticipantRepository;
        this.userService = userService;
    }

    public List<Lobby> getLobbies() {
        return lobbyRepository.findAll();
    }

    public Lobby createLobby(String playerToken, Integer capacity, String lobbyPassword, String lobbyName) {
        User host = userService.authenticate(playerToken);

        Lobby lobby = new Lobby();
        lobby.setCapacity(resolveCapacity(capacity));
        lobby.setHostId(host.getId());
        lobby.setGameId(null);

        if (lobbyPassword != null && !lobbyPassword.isBlank()) {
            lobby.setPassword(lobbyPassword.trim());
        }

        if (lobbyName == null || lobbyName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby name must not be empty.");
        }
        lobby.setName(lobbyName.trim());
    
        Lobby createdLobby = lobbyRepository.save(lobby);
        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setLobby(createdLobby);
        hostParticipant.setUser(host);
        hostParticipant.setHost(true);
        hostParticipant.setBot(false);
        lobbyParticipantRepository.save(hostParticipant);

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

        boolean alreadyJoined = lobby.getParticipants().stream()
                .anyMatch(participant -> 
                    participant.getUser() != null &&
                    participant.getUser().getId().equals(user.getId()));
        if (alreadyJoined) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User with id " + user.getId() + " already joined lobby " + lobbyId + ".");
        }

        if (lobby.getCurrentParticipants() >= lobby.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lobby with id " + lobbyId + " is full.");
        }

        LobbyParticipant participant = new LobbyParticipant();
        participant.setLobby(lobby);
        participant.setUser(user);
        participant.setHost(false);
        participant.setBot(false);
        lobbyParticipantRepository.save(participant);

        lobbyRepository.flush();
        return lobby;
    }

    public Game startGame (Long lobbyId, String playerToken) {
        User requester = userService.authenticate(playerToken);

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        boolean requesterInLobby = lobby.getParticipants().stream()
            .anyMatch(participant -> 
                participant.getUser() != null &&
                participant.getUser().getId().equals(requester.getId()));
                
        if (!requesterInLobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this lobby");
        }

        if (!lobby.getHostId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the game");
        }

        if (lobby.getCurrentParticipants() < 3) {
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

        List<LobbyParticipant> orderedParticipants = lobby.getParticipants().stream()
            .filter(participant -> participant.getUser() != null)
            .sorted((p1, p2) -> p1.getUser().getId().compareTo(p2.getUser().getId()))
            .toList();

        String[] colors = {"RED", "BLUE", "WHITE", "ORANGE"};

        for (int i = 0; i < orderedParticipants.size(); i++) {
            Player player = new Player();
            player.setUser(orderedParticipants.get(i).getUser());
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

    public Lobby getLobbyById(Long lobbyId, String playerToken) {
        userService.authenticate(playerToken);

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));
        return lobby;
    }


    private int resolveCapacity(Integer capacity) {
        int resolved = capacity == null ? DEFAULT_LOBBY_CAPACITY : capacity;
        if (resolved < 2 || resolved > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby capacity must be between 2 and 4.");
        }
        return resolved;
    }

    
}
