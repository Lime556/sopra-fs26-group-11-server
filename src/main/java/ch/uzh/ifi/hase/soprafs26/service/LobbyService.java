package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
import ch.uzh.ifi.hase.soprafs26.repository.LobbyInvitationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyParticipantRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PlayerRepository;

@Service
@Transactional
public class LobbyService {



    private static final int DEFAULT_LOBBY_CAPACITY = 4;
    private static final int MIN_PLAYERS_TO_START = 2;
    private static final long LOBBY_DISCONNECT_GRACE_SECONDS = 40;
    private static final long ACTIVE_GAME_REJOIN_GRACE_SECONDS = 300;

    private final GameRepository gameRepository;
    private final LobbyRepository lobbyRepository;
    private final PlayerRepository playerRepository;
    private final UserService userService;
    private final LobbyParticipantRepository lobbyParticipantRepository;
    private final LobbyInvitationRepository lobbyInvitationRepository;
    
    public LobbyService(
        LobbyRepository lobbyRepository,
        GameRepository gameRepository,
        PlayerRepository playerRepository,
        LobbyParticipantRepository lobbyParticipantRepository,
        LobbyInvitationRepository lobbyInvitationRepository,
        UserService userService
    ) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.lobbyParticipantRepository = lobbyParticipantRepository;
        this.lobbyInvitationRepository = lobbyInvitationRepository;
        this.userService = userService;
    }

    public List<Lobby> getLobbies() {
        List<Lobby> visibleLobbies = new ArrayList<>();
        for (Lobby lobby : lobbyRepository.findAll()) {
            if (!evictDisconnectedParticipants(lobby, null)) {
                visibleLobbies.add(lobby);
            }
        }
        return visibleLobbies;
    }

    public Lobby createLobby(String playerToken, Integer capacity, String lobbyPassword, String lobbyName) {
        User hostUser = userService.authenticate(playerToken);
        ensureUserNotInAnyLobby(hostUser);

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
        markParticipantOnline(hostParticipant);
        hostParticipant = lobbyParticipantRepository.saveAndFlush(hostParticipant);

        lobby.setHostParticipant(hostParticipant);

        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby joinLobby(Long lobbyId, String playerToken, String lobbyPassword) {
        User user = userService.authenticate(playerToken);

        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant existingParticipant = findExistingParticipantForJoin(lobby, user);
        if (existingParticipant != null) {
            ensureStartedGameReconnectAllowed(lobby, existingParticipant);
            markParticipantOnline(existingParticipant);
            lobbyParticipantRepository.saveAndFlush(existingParticipant);
            return lobby;
        }
      
        ensureLobbyGameAcceptsNewParticipant(lobby);
        validateLobbyPassword(lobby, lobbyPassword);
        ensureLobbyNotFull(lobby);

        LobbyParticipant participant = new LobbyParticipant();
        participant.setLobby(lobby);
        participant.setUser(user);
        participant.setBot(false);
        markParticipantOnline(participant);
        lobby.getParticipants().add(participant);
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

        List<LobbyParticipant> humanParticipants = lobby.getParticipants().stream()
            .filter(participant -> !participant.isBot())
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(humanParticipants);

        List<LobbyParticipant> botParticipants = lobby.getParticipants().stream()
            .filter(LobbyParticipant::isBot)
            .sorted(Comparator.comparing(participant -> participant.getId() == null ? Long.MAX_VALUE : participant.getId()))
            .toList();

        List<LobbyParticipant> orderedParticipants = new ArrayList<>(humanParticipants);
        orderedParticipants.addAll(botParticipants);

        String[] colors = {"#b4233a", "#2e7ccf", "#e0a120", "#166534"};
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
            player.setOnline(true);
            player.setLastSeenAt(Instant.now());
            player.setDisconnectedAt(null);
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
        markParticipantOnline(botParticipant);
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
        User requester = userService.authenticate(playerToken);
        Lobby lobby = findLobbyOrThrow(lobbyId);
        if (evictDisconnectedParticipants(lobby, requester.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Lobby with id " + lobbyId + " was not found.");
        }
        return lobbyRepository.saveAndFlush(lobby);
    }

    public Lobby heartbeatLobby(Long lobbyId, String playerToken) {
        User user = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        LobbyParticipant participant = findParticipantByUserOrThrow(lobby, user);
        markParticipantOnline(participant);
        if (evictDisconnectedParticipants(lobby, user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Lobby with id " + lobbyId + " was not found.");
        }
        return lobbyRepository.saveAndFlush(lobby);
    }

    public void leaveLobby(Long lobbyId, String playerToken) {
        User user = userService.authenticate(playerToken);
        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        if (lobby.getGameId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot leave lobby after the game has started.");
        }

        LobbyParticipant participant = lobby.getParticipants().stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this lobby"));

        lobby.getParticipants().remove(participant);
        lobbyParticipantRepository.delete(participant);

        if (lobby.getParticipants().isEmpty()) {
            deleteLobbyAndAssociations(lobby);
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
        deleteLobbyAndAssociations(lobby);
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

    private void ensureUserNotInAnyLobby(User user) {
        if (user.getId() == null) {
            return;
        }

        for (LobbyParticipant participant : lobbyParticipantRepository.findByUser_Id(user.getId())) {
            if (blocksNewLobbyMembership(participant)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "User with id " + user.getId() + " is already part of an active lobby or game."
                );
            }
        }

        ensureUserNotInAnyActiveGame(user, null);
    }

    private LobbyParticipant findExistingParticipantForJoin(Lobby lobby, User user) {
        if (user.getId() == null) {
            return null;
        }

        List<LobbyParticipant> existingParticipants = lobbyParticipantRepository.findByUser_Id(user.getId());
        LobbyParticipant sameLobbyParticipant = null;

        for (LobbyParticipant participant : existingParticipants) {
            Long existingLobbyId = participant.getLobby() != null ? participant.getLobby().getId() : null;
            if (lobby.getId().equals(existingLobbyId)) {
                sameLobbyParticipant = participant;
            }
            else if (blocksNewLobbyMembership(participant)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "User with id " + user.getId() + " is already part of an active lobby or game."
                );
            }
        }

        Long allowedGameId = sameLobbyParticipant != null ? lobby.getGameId() : null;
        ensureUserNotInAnyActiveGame(user, allowedGameId);

        return sameLobbyParticipant;
    }

    private boolean blocksNewLobbyMembership(LobbyParticipant participant) {
        if (participant == null || participant.getLobby() == null) {
            return false;
        }

        Long gameId = participant.getLobby().getGameId();
        if (gameId == null) {
            return true;
        }

        return gameRepository.findById(gameId)
                .map(game -> game.getFinishedAt() == null && participantIsPlayerInGame(participant, game))
                .orElse(false);
    }

    private void ensureLobbyGameAcceptsNewParticipant(Lobby lobby) {
        Long gameId = lobby == null ? null : lobby.getGameId();
        if (gameId == null) {
            return;
        }

        boolean activeGame = gameRepository.findById(gameId)
                .map(game -> game.getFinishedAt() == null)
                .orElse(false);
        if (activeGame) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already started");
        }
    }

    private void ensureStartedGameReconnectAllowed(Lobby lobby, LobbyParticipant participant) {
        Long gameId = lobby == null ? null : lobby.getGameId();
        if (gameId == null) {
            return;
        }

        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getFinishedAt() != null) {
            return;
        }

        Long userId = participant.getUser() == null ? null : participant.getUser().getId();
        Player player = userId == null || game.getPlayers() == null
                ? null
                : game.getPlayers().stream()
                        .filter(candidate -> candidate != null && candidate.getUser() != null)
                        .filter(candidate -> userId.equals(candidate.getUser().getId()))
                        .findFirst()
                        .orElse(null);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already started");
        }

        Instant rejoinSince = player.getDisconnectedAt() != null ? player.getDisconnectedAt() : player.getLastSeenAt();
        if (rejoinSince == null) {
            rejoinSince = participant.getLastSeenAt();
        }

        if (rejoinSince != null
                && !rejoinSince.plusSeconds(ACTIVE_GAME_REJOIN_GRACE_SECONDS).isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already started");
        }
    }

    private boolean participantIsPlayerInGame(LobbyParticipant participant, Game game) {
        if (participant.getUser() == null || participant.getUser().getId() == null || game.getPlayers() == null) {
            return false;
        }

        Long userId = participant.getUser().getId();
        return game.getPlayers().stream()
                .filter(player -> player != null && player.getUser() != null)
                .anyMatch(player -> userId.equals(player.getUser().getId()));
    }

    private void ensureUserNotInAnyActiveGame(User user, Long allowedGameId) {
        for (Game game : gameRepository.findAll()) {
            if (game == null || game.getFinishedAt() != null || game.getPlayers() == null) {
                continue;
            }
            if (allowedGameId != null && allowedGameId.equals(game.getId())) {
                continue;
            }

            boolean userIsActivePlayer = game.getPlayers().stream()
                    .filter(player -> player != null && player.getUser() != null)
                    .anyMatch(player -> user.getId().equals(player.getUser().getId()));
            if (userIsActivePlayer) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "User with id " + user.getId() + " is already part of an active lobby or game."
                );
            }
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

    private void markParticipantOnline(LobbyParticipant participant) {
        participant.setOnline(true);
        participant.setLastSeenAt(Instant.now());
    }

    private boolean evictDisconnectedParticipants(Lobby lobby, Long protectedUserId) {
        if (lobby.getGameId() != null) {
            return false;
        }

        Instant cutoff = Instant.now().minusSeconds(LOBBY_DISCONNECT_GRACE_SECONDS);
        List<LobbyParticipant> disconnected = lobby.getParticipants().stream()
                .filter(participant -> !participant.isBot())
                .filter(participant -> participant.getUser() != null)
                .filter(participant -> protectedUserId == null || !protectedUserId.equals(participant.getUser().getId()))
                .filter(participant -> participant.getLastSeenAt() != null && participant.getLastSeenAt().isBefore(cutoff))
                .toList();

        for (LobbyParticipant participant : disconnected) {
            lobby.getParticipants().remove(participant);
            lobbyParticipantRepository.delete(participant);
        }

        if (!hasOnlineHumanParticipant(lobby)) {
            for (LobbyParticipant participant : new HashSet<>(lobby.getParticipants())) {
                lobbyParticipantRepository.delete(participant);
            }
            lobby.getParticipants().clear();
            lobby.setHostParticipant(null);
            deleteLobbyAndAssociations(lobby);
            return true;
        }

        boolean hostStillPresent = lobby.getHostParticipant() != null
                && lobby.getParticipants().stream()
                        .anyMatch(participant -> participant.getId().equals(lobby.getHostParticipant().getId()));
        if (!hostStillPresent) {
            LobbyParticipant newHost = lobby.getParticipants().stream()
                    .min(Comparator.comparing(participant -> participant.getId() == null ? Long.MAX_VALUE : participant.getId()))
                    .orElse(null);
            lobby.setHostParticipant(newHost);
        }
        return false;
    }

    private boolean hasOnlineHumanParticipant(Lobby lobby) {
        return lobby.getParticipants().stream()
                .anyMatch(participant -> !participant.isBot()
                        && participant.getUser() != null
                        && participant.isOnline());
    }

    private void deleteLobbyAndAssociations(Lobby lobby) {
        lobby.setHostParticipant(null);
        lobbyInvitationRepository.deleteByLobby(lobby);
        lobbyRepository.delete(lobby);
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
