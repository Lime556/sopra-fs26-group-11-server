package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyParticipantRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PlayerRepository;

@Service
@Transactional
public class LobbyService {



    private static final int DEFAULT_LOBBY_CAPACITY = 4;
    private static final int MIN_PLAYERS_TO_START = 2;

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
        User hostUser = userService.authenticate(playerToken);

        Lobby lobby = new Lobby();
        lobby.setCapacity(resolveCapacity(capacity));
        lobby.setGameId(null);
        lobby.setPassword(normalizePassword(lobbyPassword));
        lobby.setName(validateAndNormalizeLobbyName(lobbyName));
    
        lobby = lobbyRepository.saveAndFlush(lobby);

        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setLobby(lobby);
        hostParticipant.setUser(hostUser);
        hostParticipant.setBot(false);
        hostParticipant = lobbyParticipantRepository.saveAndFlush(hostParticipant);

        lobby.setHostParticipant(hostParticipant);

        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby joinLobby(Long lobbyId, String playerToken, String lobbyPassword) {
        User user = userService.authenticate(playerToken);

        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));
      
        validateLobbyPassword(lobby, lobbyPassword);
        ensureUserNotAlreadyInLobby(lobby, user);
        ensureLobbyNotFull(lobby);

        LobbyParticipant participant = new LobbyParticipant();
        participant.setLobby(lobby);
        participant.setUser(user);
        participant.setBot(false);
        lobbyParticipantRepository.saveAndFlush(participant);

        return lobby;
    }

    public Game startGame (Long lobbyId, String playerToken) {
        User requester = userService.authenticate(playerToken);
        Lobby lobby = findLobbyOrThrow(lobbyId);

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
                
        if (lobby.getHostParticipant() == null || !lobby.getHostParticipant().getId().equals(requesterParticipant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the game");
        }

        if (lobby.getCurrentParticipants() < MIN_PLAYERS_TO_START) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough players");
        }

        if (lobby.getGameId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already started");
        }

        Board board = new Board();
        board.generateBoard();

        Game game = new Game();
        game.setBoard(board);
        game.setCurrentTurnIndex(0);
        game.setDiceValue(null);
        int desertIndex = board.getHexTiles() != null ? board.getHexTiles().indexOf("DESERT") : -1;
        game.setRobberTileIndex(desertIndex >= 0 ? desertIndex + 1 : null);
        game.setDevelopmentKnightRemaining(14);
        game.setDevelopmentVictoryPointRemaining(5);
        game.setDevelopmentRoadBuildingRemaining(2);
        game.setDevelopmentYearOfPlentyRemaining(2);
        game.setDevelopmentMonopolyRemaining(2);
        game.setTargetVictoryPoints(10);
        game.setGamePhase("SETUP");
        game.setStartedAt(java.time.LocalDateTime.now());
        game.setFinishedAt(null);
        game = gameRepository.saveAndFlush(game);

        List<LobbyParticipant> orderedParticipants = lobby.getParticipants().stream()
            .sorted(Comparator
                .comparing((LobbyParticipant participant) -> participant.isBot())
                .thenComparing(participant -> participant.getUser() != null ? participant.getUser().getId() : Long.MAX_VALUE)
                .thenComparing(participant -> participant.getId() != null ? participant.getId() : Long.MAX_VALUE))
            .toList();

        String[] colors = {"RED", "BLUE", "WHITE", "ORANGE"};
        List<Player> gamePlayers = new ArrayList<>();
        int botCount = 0;

        for (int i = 0; i < orderedParticipants.size(); i++) {
            LobbyParticipant participant = orderedParticipants.get(i);
            if (participant.isBot()) {
                botCount++;
            }

            Player player = new Player();
            player.setUser(participant.getUser());
            player.setGameId(game.getId());
            player.setColor(colors[i]);
            player.setBot(participant.isBot());
            player.setName(resolvePlayerName(participant, i, botCount));
            player.setVictoryPoints(0);
            player.setWood(0);
            player.setBrick(0);
            player.setWool(0);
            player.setWheat(0);
            player.setOre(0);
            Player savedPlayer = playerRepository.save(player);
            gamePlayers.add(savedPlayer);
        }

        game.setPlayers(gamePlayers);
        gameRepository.saveAndFlush(game);

        lobby.setGameId(game.getId());
        lobbyRepository.saveAndFlush(lobby);

        return game;
    }

    public Lobby addBot(Long lobbyId, String playerToken) {
        User requester = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
        ensureRequesterIsHost(lobby, requesterParticipant);
        ensureLobbyNotFull(lobby);

        if (lobby.getGameId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already started");
        }

        LobbyParticipant botParticipant = new LobbyParticipant();
        botParticipant.setLobby(lobby);
        botParticipant.setUser(null);
        botParticipant.setBot(true);
        botParticipant = lobbyParticipantRepository.saveAndFlush(botParticipant);
        lobby.getParticipants().add(botParticipant);

        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby removeBot(Long lobbyId, String playerToken, Long participantId) {
        User requester = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
        ensureRequesterIsHost(lobby, requesterParticipant);

        if (lobby.getGameId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already started");
        }

        LobbyParticipant targetParticipant = findParticipantByIdOrThrow(lobby, participantId);
        if (!targetParticipant.isBot()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Participant is not a bot.");
        }

        lobby.getParticipants().remove(targetParticipant);
        lobbyParticipantRepository.delete(targetParticipant);

        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby getLobbyById(Long lobbyId, String playerToken) {
        userService.authenticate(playerToken);
        return findLobbyOrThrow(lobbyId);
    }

    public void leaveLobby(Long lobbyId, String playerToken) {
        User user = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant participant = lobby.getParticipants().stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this lobby"));

        lobby.getParticipants().remove(participant);
        lobbyParticipantRepository.delete(participant);

        if (lobby.getParticipants().isEmpty()) {
            lobbyRepository.delete(lobby);
            return;
        }

        if (lobby.getHostParticipant() != null && lobby.getHostParticipant().getId().equals(participant.getId())) {
            LobbyParticipant newHost = lobby.getParticipants().iterator().next();
            lobby.setHostParticipant(newHost);
        }

        lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby kickParticipant(Long lobbyId, String playerToken, Long participantId) {
        User requester = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
        ensureRequesterIsHost(lobby, requesterParticipant);

        LobbyParticipant targetParticipant = findParticipantByIdOrThrow(lobby, participantId);
        if (requesterParticipant.getId().equals(targetParticipant.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Host cannot kick themselves.");
        }

        lobby.getParticipants().remove(targetParticipant);
        lobbyParticipantRepository.delete(targetParticipant);

        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby kickPlayer(Long lobbyId, String playerToken, Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }

        User requester = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
        ensureRequesterIsHost(lobby, requesterParticipant);

        LobbyParticipant targetParticipant = findParticipantByUserIdOrThrow(lobby, userId);
        if (requesterParticipant.getId().equals(targetParticipant.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Host cannot kick themselves.");
        }

        lobby.getParticipants().remove(targetParticipant);
        lobbyParticipantRepository.delete(targetParticipant);

        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby transferHost(Long lobbyId, String playerToken, Long newHostParticipantId) {
        if (newHostParticipantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newHostParticipantId is required.");
        }

        User requester = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
        ensureRequesterIsHost(lobby, requesterParticipant);

        LobbyParticipant newHostParticipant = findParticipantByIdOrThrow(lobby, newHostParticipantId);
        lobby.setHostParticipant(newHostParticipant);
        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby transferHostToUser(Long lobbyId, String playerToken, Long newHostUserId) {
        if (newHostUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }

        User requester = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
        ensureRequesterIsHost(lobby, requesterParticipant);

        LobbyParticipant newHostParticipant = findParticipantByUserIdOrThrow(lobby, newHostUserId);
        lobby.setHostParticipant(newHostParticipant);
        return lobbyRepository.saveAndFlush(lobby);
    }

    public void closeLobby(Long lobbyId, String playerToken) {
        User requester = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant requesterParticipant = findParticipantByUserOrThrow(lobby, requester);
        ensureRequesterIsHost(lobby, requesterParticipant);

        for (LobbyParticipant participant : new HashSet<>(lobby.getParticipants())) {
            lobbyParticipantRepository.delete(participant);
        }
        lobby.getParticipants().clear();
        lobbyRepository.delete(lobby);
    }


    private int resolveCapacity(Integer capacity) {
        int resolved = capacity == null ? DEFAULT_LOBBY_CAPACITY : capacity;
        if (resolved < 2 || resolved > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby capacity must be between 2 and 4.");
        }
        return resolved;
    }


    private Lobby findLobbyOrThrow(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."
                ));
    }

    private String validateAndNormalizeLobbyName(String lobbyName) {
        if (lobbyName == null || lobbyName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby name must not be empty.");
        }
        return lobbyName.trim();
    }

    private String normalizePassword(String lobbyPassword) {
        if (lobbyPassword == null || lobbyPassword.isBlank()) {
            return null;
        }
        return lobbyPassword.trim();
    }

    private void validateLobbyPassword(Lobby lobby, String lobbyPassword) {
        if (lobby.getPassword() != null && !lobby.getPassword().isBlank()) {
            if (lobbyPassword == null || !lobby.getPassword().equals(lobbyPassword)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Wrong lobby password for lobby " + lobby.getId() + "."
                );
            }
        }
    }

    private void ensureUserNotAlreadyInLobby(Lobby lobby, User user) {
        boolean alreadyJoined = lobby.getParticipants().stream()
                .anyMatch(participant ->
                        participant.getUser() != null
                                && participant.getUser().getId().equals(user.getId()));
    
        if (alreadyJoined) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User with id " + user.getId() + " already joined lobby " + lobby.getId() + "."
            );
        }
    }

    private void ensureLobbyNotFull(Lobby lobby) {
        if (lobby.getCurrentParticipants() >= lobby.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Lobby with id " + lobby.getId() + " is full."
            );
        }
    }

    private String resolvePlayerName(LobbyParticipant participant, int index, int botCount) {
        if (participant.getUser() != null) {
            return participant.getUser().getUsername();
        }
        if (participant.isBot()) {
            return "Bot " + botCount;
        }
        return "Player " + (index + 1);
    }

    private LobbyParticipant findParticipantByUserOrThrow(Lobby lobby, User user) {
        return lobby.getParticipants().stream()
                .filter(participant ->
                        participant.getUser() != null
                                && participant.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "User is not part of this lobby"
                ));
    }

    private LobbyParticipant findParticipantByIdOrThrow(Lobby lobby, Long participantId) {
        return lobby.getParticipants().stream()
                .filter(participant -> participant.getId() != null && participant.getId().equals(participantId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found in lobby."));
    }

    private LobbyParticipant findParticipantByUserIdOrThrow(Lobby lobby, Long userId) {
        return lobby.getParticipants().stream()
                .filter(participant -> participant.getUser() != null
                        && participant.getUser().getId() != null
                        && participant.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not part of this lobby."));
    }

    private void ensureRequesterIsHost(Lobby lobby, LobbyParticipant requesterParticipant) {
        if (lobby.getHostParticipant() == null || requesterParticipant.getId() == null
                || !requesterParticipant.getId().equals(lobby.getHostParticipant().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can manage lobby participants.");
        }
    }

    
}
