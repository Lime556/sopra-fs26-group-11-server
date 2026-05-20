package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyParticipantRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PlayerRepository;

public class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserService userService;

    @Mock
    private LobbyParticipantRepository lobbyParticipantRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private LobbyService lobbyService;

    private User host;
    private Lobby lobby;
    private User user;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        host = new User();
        host.setId(10L);
        host.setToken("valid-token");
        host.setEmail("host@email.com");

        user = new User();
        user.setId(11L);
        user.setToken("valid-token");
        user.setEmail("user@email.com");

        lobby = new Lobby();
        lobby.setId(1L);
        lobby.setCapacity(2);
        lobby.setParticipants(new HashSet<>());

        Mockito.when(lobbyRepository.saveAndFlush(Mockito.any(Lobby.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(lobbyParticipantRepository.saveAndFlush(Mockito.any(LobbyParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(gameRepository.saveAndFlush(Mockito.any(Game.class)))
                .thenAnswer(invocation -> {
                    Game game = invocation.getArgument(0);
                    if (game.getId() == null) {
                        game.setId(999L);
                    }
                    return game;
                });

        Mockito.when(playerRepository.save(Mockito.any(Player.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(gameRepository.findAll()).thenReturn(List.of());
    }

    @Test
    public void createLobby_validInput_success() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);

        Lobby createdLobby = lobbyService.createLobby("valid-token", 4, null, "Test Lobby");

        assertEquals("Test Lobby", createdLobby.getName());
        assertEquals(4, createdLobby.getCapacity());
        assertNotNull(createdLobby.getHostParticipant());
        assertEquals(host.getId(), createdLobby.getHostParticipant().getUser().getId());
    }

    @Test
    public void createLobby_privateLobby_setsPassword_success() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);

        Lobby createdLobby = lobbyService.createLobby("valid-token", 4, "secret", "Private Lobby");

        assertEquals("Private Lobby", createdLobby.getName());
        assertEquals("secret", createdLobby.getPassword());
    }

    @Test
    public void createLobby_invalidCapacity_throwsBadRequest() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby("valid-token", 7, null, "Test Lobby"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void createLobby_missingToken_throwsUnauthorized() {
        Mockito.when(userService.authenticate(null))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby(null, 4, null, "Test Lobby"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void createLobby_userAlreadyInLobby_throwsConflict() {
        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(host);
        existingParticipant.setLobby(lobby);
        existingParticipant.setBot(false);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyParticipantRepository.findByUser_Id(host.getId())).thenReturn(List.of(existingParticipant));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby("valid-token", 4, null, "Test Lobby"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void createLobby_userOnlyInFinishedGameLobby_success() {
        Lobby oldLobby = new Lobby();
        oldLobby.setId(2L);
        oldLobby.setGameId(99L);

        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(host);
        existingParticipant.setLobby(oldLobby);
        existingParticipant.setBot(false);

        Game finishedGame = new Game();
        finishedGame.setId(99L);
        finishedGame.setFinishedAt(LocalDateTime.now());

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyParticipantRepository.findByUser_Id(host.getId())).thenReturn(List.of(existingParticipant));
        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(finishedGame));

        Lobby createdLobby = lobbyService.createLobby("valid-token", 4, null, "Test Lobby");

        assertEquals("Test Lobby", createdLobby.getName());
        assertNotNull(createdLobby.getHostParticipant());
    }

    @Test
    public void heartbeatLobby_refreshesRequesterAndEvictsDisconnectedParticipant() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);

        Lobby testLobby = new Lobby();
        testLobby.setId(20L);
        testLobby.setCapacity(4);
        testLobby.setParticipants(new HashSet<>());

        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setLobby(testLobby);
        hostParticipant.setUser(host);
        hostParticipant.setBot(false);
        hostParticipant.setOnline(true);
        hostParticipant.setLastSeenAt(Instant.now().minusSeconds(10));

        User inactiveUser = new User();
        inactiveUser.setId(12L);
        inactiveUser.setEmail("inactive@email.com");

        LobbyParticipant inactiveParticipant = new LobbyParticipant();
        inactiveParticipant.setId(101L);
        inactiveParticipant.setLobby(testLobby);
        inactiveParticipant.setUser(inactiveUser);
        inactiveParticipant.setBot(false);
        inactiveParticipant.setOnline(true);
        inactiveParticipant.setLastSeenAt(Instant.now().minusSeconds(60));

        testLobby.getParticipants().add(hostParticipant);
        testLobby.getParticipants().add(inactiveParticipant);
        testLobby.setHostParticipant(hostParticipant);

        Mockito.when(lobbyRepository.findByIdWithLock(20L)).thenReturn(Optional.of(testLobby));

        Lobby result = lobbyService.heartbeatLobby(20L, "valid-token");

        assertTrue(hostParticipant.isOnline());
        assertNotNull(hostParticipant.getLastSeenAt());
        assertEquals(1, result.getParticipants().size());
        assertTrue(result.getParticipants().contains(hostParticipant));
        assertFalse(result.getParticipants().contains(inactiveParticipant));
        Mockito.verify(lobbyParticipantRepository).delete(inactiveParticipant);
        Mockito.verify(lobbyRepository).saveAndFlush(testLobby);
    }

    @Test
    public void getLobbies_returnsAll() {
        Lobby first = new Lobby();
        first.setId(1L);
        first.setParticipants(new HashSet<>());
        LobbyParticipant firstParticipant = new LobbyParticipant();
        firstParticipant.setId(1L);
        firstParticipant.setLobby(first);
        firstParticipant.setUser(host);
        firstParticipant.setBot(false);
        firstParticipant.setOnline(true);
        firstParticipant.setLastSeenAt(Instant.now());
        first.getParticipants().add(firstParticipant);

        Lobby second = new Lobby();
        second.setId(2L);
        second.setParticipants(new HashSet<>());
        LobbyParticipant secondParticipant = new LobbyParticipant();
        secondParticipant.setId(2L);
        secondParticipant.setLobby(second);
        secondParticipant.setUser(user);
        secondParticipant.setBot(false);
        secondParticipant.setOnline(true);
        secondParticipant.setLastSeenAt(Instant.now());
        second.getParticipants().add(secondParticipant);

        Mockito.when(lobbyRepository.findAll()).thenReturn(List.of(first, second));

        List<Lobby> lobbies = lobbyService.getLobbies();

        assertEquals(2, lobbies.size());
    }

    @Test
    public void getLobbies_deletesPreGameLobbyWithoutOnlineHumanParticipants() {
        Lobby abandonedLobby = new Lobby();
        abandonedLobby.setId(30L);
        abandonedLobby.setCapacity(4);
        abandonedLobby.setGameId(null);
        abandonedLobby.setParticipants(new HashSet<>());

        User staleUser = new User();
        staleUser.setId(30L);
        staleUser.setEmail("stale@email.com");

        LobbyParticipant staleParticipant = new LobbyParticipant();
        staleParticipant.setId(300L);
        staleParticipant.setLobby(abandonedLobby);
        staleParticipant.setUser(staleUser);
        staleParticipant.setBot(false);
        staleParticipant.setOnline(true);
        staleParticipant.setLastSeenAt(Instant.now().minusSeconds(60));

        LobbyParticipant botParticipant = new LobbyParticipant();
        botParticipant.setId(301L);
        botParticipant.setLobby(abandonedLobby);
        botParticipant.setUser(null);
        botParticipant.setBot(true);
        botParticipant.setOnline(true);
        botParticipant.setLastSeenAt(Instant.now());

        abandonedLobby.getParticipants().add(staleParticipant);
        abandonedLobby.getParticipants().add(botParticipant);
        abandonedLobby.setHostParticipant(staleParticipant);

        Mockito.when(lobbyRepository.findAll()).thenReturn(List.of(abandonedLobby));

        List<Lobby> lobbies = lobbyService.getLobbies();

        assertTrue(lobbies.isEmpty());
        assertTrue(abandonedLobby.getParticipants().isEmpty());
        assertEquals(null, abandonedLobby.getHostParticipant());
        Mockito.verify(lobbyParticipantRepository).delete(staleParticipant);
        Mockito.verify(lobbyParticipantRepository).delete(botParticipant);
        Mockito.verify(lobbyRepository).delete(abandonedLobby);
    }

    @Test
    public void joinLobby_validInputs_success() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.joinLobby(1L, "valid-token", null);

        Mockito.verify(lobbyParticipantRepository, Mockito.times(1))
                .saveAndFlush(Mockito.any(LobbyParticipant.class));
        assertEquals(lobby.getId(), updatedLobby.getId());
    }

    @Test
    public void joinLobby_protectedLobby_correctPassword_success() {
        lobby.setPassword("secret");
        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.joinLobby(1L, "valid-token", "secret");

        Mockito.verify(lobbyParticipantRepository, Mockito.times(1))
                .saveAndFlush(Mockito.any(LobbyParticipant.class));
        assertEquals(lobby.getId(), updatedLobby.getId());
    }

    @Test
    public void joinLobby_missingToken_throwsUnauthorized() {
        Mockito.when(userService.authenticate(null))
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, null, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void joinLobby_lobbyNotFound_throwsNotFound() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void joinLobby_wrongPassword_throwsForbidden() {
        lobby.setPassword("secret");

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", "wrong"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void joinLobby_lobbyFull_throwsConflict() {
        User existingUserA = new User();
        existingUserA.setId(101L);
    
        User existingUserB = new User();
        existingUserB.setId(102L);
    
        LobbyParticipant existingA = new LobbyParticipant();
        existingA.setId(201L);
        existingA.setUser(existingUserA);
        existingA.setLobby(lobby);
        existingA.setBot(false);
    
        LobbyParticipant existingB = new LobbyParticipant();
        existingB.setId(202L);
        existingB.setUser(existingUserB);
        existingB.setLobby(lobby);
        existingB.setBot(false);
    

        lobby.getParticipants().add(existingA);
        lobby.getParticipants().add(existingB);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void joinLobby_startedGameForNewParticipant_throwsConflictAndDoesNotSaveParticipant() {
        lobby.setGameId(99L);

        Game activeGame = new Game();
        activeGame.setId(99L);
        activeGame.setFinishedAt(null);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of());
        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(activeGame));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Game already started", exception.getReason());
        Mockito.verify(lobbyParticipantRepository, Mockito.never())
                .saveAndFlush(Mockito.any(LobbyParticipant.class));
    }

    @Test
    public void joinLobby_userAlreadyInSameLobby_reconnectsSuccessfully() {
        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(lobby);
        existingParticipant.setBot(false);
        existingParticipant.setOnline(false);

        lobby.getParticipants().add(existingParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of(existingParticipant));

        Lobby updatedLobby = lobbyService.joinLobby(1L, "valid-token", null);

        assertEquals(lobby.getId(), updatedLobby.getId());
        assertTrue(existingParticipant.isOnline());
        assertNotNull(existingParticipant.getLastSeenAt());
        Mockito.verify(lobbyParticipantRepository, Mockito.times(1)).saveAndFlush(existingParticipant);
    }

    @Test
    public void joinLobby_existingParticipantInStartedGameWithinGrace_reconnectsSuccessfully() {
        lobby.setGameId(99L);

        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(lobby);
        existingParticipant.setBot(false);
        existingParticipant.setOnline(false);
        existingParticipant.setLastSeenAt(Instant.now().minusSeconds(60));
        lobby.getParticipants().add(existingParticipant);

        Player player = new Player();
        player.setUser(user);
        player.setLastSeenAt(Instant.now().minusSeconds(60));

        Game activeGame = new Game();
        activeGame.setId(99L);
        activeGame.setFinishedAt(null);
        activeGame.setPlayers(List.of(player));

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of(existingParticipant));
        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(activeGame));

        Lobby updatedLobby = lobbyService.joinLobby(1L, "valid-token", null);

        assertEquals(lobby.getId(), updatedLobby.getId());
        assertTrue(existingParticipant.isOnline());
        Mockito.verify(lobbyParticipantRepository, Mockito.times(1)).saveAndFlush(existingParticipant);
    }

    @Test
    public void joinLobby_existingParticipantInStartedGameAfterGrace_throwsConflict() {
        lobby.setGameId(99L);

        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(lobby);
        existingParticipant.setBot(false);
        existingParticipant.setOnline(false);
        existingParticipant.setLastSeenAt(Instant.now().minusSeconds(360));
        lobby.getParticipants().add(existingParticipant);

        Player player = new Player();
        player.setUser(user);
        player.setDisconnectedAt(Instant.now().minusSeconds(360));

        Game activeGame = new Game();
        activeGame.setId(99L);
        activeGame.setFinishedAt(null);
        activeGame.setPlayers(List.of(player));

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of(existingParticipant));
        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(activeGame));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Game already started", exception.getReason());
        assertFalse(existingParticipant.isOnline());
        Mockito.verify(lobbyParticipantRepository, Mockito.never()).saveAndFlush(existingParticipant);
    }

    @Test
    public void joinLobby_userAlreadyInAnotherLobby_throwsConflict() {
        Lobby otherLobby = new Lobby();
        otherLobby.setId(2L);
        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(otherLobby);
        existingParticipant.setBot(false);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of(existingParticipant));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void joinLobby_userOnlyInFinishedGameLobby_success() {
        Lobby oldLobby = new Lobby();
        oldLobby.setId(2L);
        oldLobby.setGameId(99L);

        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(oldLobby);
        existingParticipant.setBot(false);

        Game finishedGame = new Game();
        finishedGame.setId(99L);
        finishedGame.setFinishedAt(LocalDateTime.now());

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of(existingParticipant));
        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(finishedGame));

        Lobby updatedLobby = lobbyService.joinLobby(1L, "valid-token", null);

        Mockito.verify(lobbyParticipantRepository, Mockito.times(1))
                .saveAndFlush(Mockito.argThat(participant -> participant.getLobby().equals(lobby)));
        assertEquals(lobby.getId(), updatedLobby.getId());
    }

    @Test
    public void joinLobby_staleStartedGameParticipantWhoIsNotPlayer_success() {
        Lobby oldLobby = new Lobby();
        oldLobby.setId(2L);
        oldLobby.setGameId(99L);

        LobbyParticipant staleParticipant = new LobbyParticipant();
        staleParticipant.setId(100L);
        staleParticipant.setUser(user);
        staleParticipant.setLobby(oldLobby);
        staleParticipant.setBot(false);

        User otherUser = new User();
        otherUser.setId(12L);
        Player otherPlayer = new Player();
        otherPlayer.setUser(otherUser);

        Game activeGame = new Game();
        activeGame.setId(99L);
        activeGame.setFinishedAt(null);
        activeGame.setPlayers(List.of(otherPlayer));

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of(staleParticipant));
        Mockito.when(gameRepository.findById(99L)).thenReturn(Optional.of(activeGame));
        Mockito.when(gameRepository.findAll()).thenReturn(List.of(activeGame));

        Lobby updatedLobby = lobbyService.joinLobby(1L, "valid-token", null);

        Mockito.verify(lobbyParticipantRepository, Mockito.times(1))
                .saveAndFlush(Mockito.argThat(participant -> participant.getLobby().equals(lobby)));
        assertEquals(lobby.getId(), updatedLobby.getId());
    }

    @Test
    public void joinLobby_userAlreadyInUnfinishedGameWithoutLobbyParticipant_throwsConflict() {
        Game activeGame = new Game();
        activeGame.setId(99L);
        activeGame.setFinishedAt(null);

        Player existingPlayer = new Player();
        existingPlayer.setUser(user);
        activeGame.setPlayers(List.of(existingPlayer));

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(lobbyParticipantRepository.findByUser_Id(user.getId())).thenReturn(List.of());
        Mockito.when(gameRepository.findAll()).thenReturn(List.of(activeGame));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void getLobbyById_validId_success() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        LobbyParticipant participant = new LobbyParticipant();
        participant.setId(1L);
        participant.setLobby(lobby);
        participant.setUser(user);
        participant.setBot(false);
        participant.setOnline(true);
        participant.setLastSeenAt(Instant.now());
        lobby.getParticipants().add(participant);

        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        Lobby result = lobbyService.getLobbyById(1L, "valid-token");

        assertEquals(1L, result.getId());
    }

    @Test
    public void getLobbyById_startedGame_doesNotEvictDisconnectedParticipant() {
        User inactiveUser = new User();
        inactiveUser.setId(12L);

        LobbyParticipant requesterParticipant = new LobbyParticipant();
        requesterParticipant.setId(100L);
        requesterParticipant.setUser(user);
        requesterParticipant.setLobby(lobby);
        requesterParticipant.setBot(false);
        requesterParticipant.setLastSeenAt(Instant.now());

        LobbyParticipant inactiveParticipant = new LobbyParticipant();
        inactiveParticipant.setId(101L);
        inactiveParticipant.setUser(inactiveUser);
        inactiveParticipant.setLobby(lobby);
        inactiveParticipant.setBot(false);
        inactiveParticipant.setLastSeenAt(Instant.now().minusSeconds(10));

        lobby.setGameId(99L);
        lobby.getParticipants().add(requesterParticipant);
        lobby.getParticipants().add(inactiveParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        Lobby result = lobbyService.getLobbyById(1L, "valid-token");

        assertTrue(result.getParticipants().contains(inactiveParticipant));
        Mockito.verify(lobbyParticipantRepository, Mockito.never()).delete(inactiveParticipant);
    }

    @Test
    public void getLobbyById_notFound_throwsNotFound() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(1L, "valid-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void getLobbyById_missingToken_throwsUnauthorized() {
        Mockito.when(userService.authenticate(null))
               .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated"));
      
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(1L, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
    
    @Test
    public void getLobbyById_invalidToken_throwsUnauthorized() {
        Mockito.when(userService.authenticate("invalid-token"))
               .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(1L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void addBot_hostAddsBot_success() {
        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setUser(host);
        hostParticipant.setLobby(lobby);
        hostParticipant.setBot(false);
        lobby.getParticipants().add(hostParticipant);
        lobby.setHostParticipant(hostParticipant);
        lobby.setCapacity(4);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.addBot(1L, "valid-token");

        assertEquals(2, updatedLobby.getCurrentParticipants());
        assertTrue(updatedLobby.getParticipants().stream().anyMatch(LobbyParticipant::isBot));
        Mockito.verify(lobbyParticipantRepository).saveAndFlush(Mockito.argThat(LobbyParticipant::isBot));
    }

    @Test
    public void removeBot_hostRemovesBot_success() {
        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setUser(host);
        hostParticipant.setLobby(lobby);
        hostParticipant.setBot(false);

        LobbyParticipant botParticipant = new LobbyParticipant();
        botParticipant.setId(101L);
        botParticipant.setUser(null);
        botParticipant.setLobby(lobby);
        botParticipant.setBot(true);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(botParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.removeBot(1L, "valid-token", 101L);

        assertEquals(1, updatedLobby.getCurrentParticipants());
        assertTrue(updatedLobby.getParticipants().stream().noneMatch(LobbyParticipant::isBot));
        Mockito.verify(lobbyParticipantRepository).delete(botParticipant);
    }

    @Test
    public void leaveLobby_participantLeaves_success() {
        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(lobby);
        existingParticipant.setBot(false);
        lobby.getParticipants().add(existingParticipant);
        lobby.setHostParticipant(existingParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        lobbyService.leaveLobby(1L, "valid-token");

        Mockito.verify(lobbyParticipantRepository, Mockito.times(1)).delete(existingParticipant);
        Mockito.verify(lobbyRepository, Mockito.times(1)).delete(lobby);
    }

    @Test
    public void leaveLobby_gameAlreadyStarted_throwsConflictAndKeepsParticipant() {
        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(lobby);
        existingParticipant.setBot(false);
        lobby.setGameId(99L);
        lobby.getParticipants().add(existingParticipant);
        lobby.setHostParticipant(existingParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.leaveLobby(1L, "valid-token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        Mockito.verify(lobbyParticipantRepository, Mockito.never()).delete(existingParticipant);
        assertTrue(lobby.getParticipants().contains(existingParticipant));
    }

    @Test
    public void kickParticipant_hostKicksOther_success() {
        User guest = new User();
        guest.setId(12L);

        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setUser(host);
        hostParticipant.setLobby(lobby);

        LobbyParticipant guestParticipant = new LobbyParticipant();
        guestParticipant.setId(101L);
        guestParticipant.setUser(guest);
        guestParticipant.setLobby(lobby);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.kickParticipant(1L, "valid-token", 101L);

        assertEquals(1, updatedLobby.getCurrentParticipants());
        Mockito.verify(lobbyParticipantRepository).delete(guestParticipant);
    }

    @Test
    public void transferHost_hostTransfers_success() {
        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setUser(host);
        hostParticipant.setLobby(lobby);

        LobbyParticipant guestParticipant = new LobbyParticipant();
        guestParticipant.setId(101L);
        guestParticipant.setUser(user);
        guestParticipant.setLobby(lobby);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.transferHost(1L, "valid-token", 101L);

        assertEquals(101L, updatedLobby.getHostParticipant().getId());
    }

    @Test
    public void kickPlayer_hostKicksByUserId_success() {
        User guest = new User();
        guest.setId(12L);

        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setUser(host);
        hostParticipant.setLobby(lobby);

        LobbyParticipant guestParticipant = new LobbyParticipant();
        guestParticipant.setId(101L);
        guestParticipant.setUser(guest);
        guestParticipant.setLobby(lobby);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.kickPlayer(1L, "valid-token", 12L);

        assertEquals(1, updatedLobby.getCurrentParticipants());
        Mockito.verify(lobbyParticipantRepository).delete(guestParticipant);
    }

    @Test
    public void transferHostToUser_hostTransfersByUserId_success() {
        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setUser(host);
        hostParticipant.setLobby(lobby);

        LobbyParticipant guestParticipant = new LobbyParticipant();
        guestParticipant.setId(101L);
        guestParticipant.setUser(user);
        guestParticipant.setLobby(lobby);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        Lobby updatedLobby = lobbyService.transferHostToUser(1L, "valid-token", 11L);

        assertEquals(101L, updatedLobby.getHostParticipant().getId());
    }

    @Test
    public void closeLobby_hostClosesLobby_success() {
        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setUser(host);
        hostParticipant.setLobby(lobby);

        LobbyParticipant guestParticipant = new LobbyParticipant();
        guestParticipant.setId(101L);
        guestParticipant.setUser(user);
        guestParticipant.setLobby(lobby);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        lobbyService.closeLobby(1L, "valid-token");

        Mockito.verify(lobbyParticipantRepository).delete(hostParticipant);
        Mockito.verify(lobbyParticipantRepository).delete(guestParticipant);
        Mockito.verify(lobbyRepository).delete(lobby);
    }

    @Test
    public void startGame_validLobby_initializesGamePlayers() {
        host.setUsername("hostUser");
        user.setUsername("guestUser");

        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setLobby(lobby);
        hostParticipant.setUser(host);
        hostParticipant.setBot(false);

        LobbyParticipant guestParticipant = new LobbyParticipant();
        guestParticipant.setId(101L);
        guestParticipant.setLobby(lobby);
        guestParticipant.setUser(user);
        guestParticipant.setBot(false);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));
        Mockito.when(playerRepository.save(Mockito.any(Player.class))).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            if (player.getId() == null) {
                player.setId(System.nanoTime());
            }
            return player;
        });

        Game game = lobbyService.startGame(1L, "valid-token");

        assertNotNull(game.getPlayers());
        assertEquals(2, game.getPlayers().size());
        assertTrue(game.getPlayers().stream().allMatch(player -> player.getGameId().equals(game.getId())));
        assertTrue(game.getPlayers().stream().anyMatch(player -> "hostUser".equals(player.getName())));
        assertTrue(game.getPlayers().stream().anyMatch(player -> "guestUser".equals(player.getName())));
        assertEquals(14, game.getDevelopmentKnightRemaining());
        assertEquals(5, game.getDevelopmentVictoryPointRemaining());
        assertEquals(2, game.getDevelopmentRoadBuildingRemaining());
        assertEquals(2, game.getDevelopmentYearOfPlentyRemaining());
        assertEquals(2, game.getDevelopmentMonopolyRemaining());
        assertEquals(game.getId(), lobby.getGameId());
    }

    @Test
    public void startGame_lobbyWithBot_includesBotPlayer() {
        host.setUsername("hostUser");

        LobbyParticipant hostParticipant = new LobbyParticipant();
        hostParticipant.setId(100L);
        hostParticipant.setLobby(lobby);
        hostParticipant.setUser(host);
        hostParticipant.setBot(false);

        LobbyParticipant botParticipant = new LobbyParticipant();
        botParticipant.setId(101L);
        botParticipant.setLobby(lobby);
        botParticipant.setUser(null);
        botParticipant.setBot(true);

        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(botParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        Game game = lobbyService.startGame(1L, "valid-token");

        assertNotNull(game.getPlayers());
        assertEquals(2, game.getPlayers().size());
        assertTrue(game.getPlayers().stream().anyMatch(player -> "hostUser".equals(player.getName()) && !player.isBot()));
        assertTrue(game.getPlayers().stream().anyMatch(player -> player.isBot() && player.getUser() == null));
    }

    @Test
    public void createLobby_defaultCapacityBlankPasswordAndNullUserId_success() {
        User anonymousHost = new User();
        anonymousHost.setToken("valid-token");
        anonymousHost.setUsername("anonymous");

        Mockito.when(userService.authenticate("valid-token")).thenReturn(anonymousHost);

        Lobby createdLobby = lobbyService.createLobby("valid-token", null, "   ", "  Trimmed Lobby  ");

        assertEquals("Trimmed Lobby", createdLobby.getName());
        assertEquals(4, createdLobby.getCapacity());
        assertEquals(null, createdLobby.getPassword());
        Mockito.verify(lobbyParticipantRepository, Mockito.never()).findByUser_Id(Mockito.anyLong());
    }

    @Test
    public void createLobby_blankLobbyName_throwsBadRequest() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobby("valid-token", 4, null, "   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void startGame_withoutHostOrWithInvalidState_throwsExpectedErrors() {
        LobbyParticipant requesterParticipant = participant(100L, host, false);
        lobby.getParticipants().add(requesterParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException noHost = assertThrows(ResponseStatusException.class,
                () -> lobbyService.startGame(1L, "valid-token"));
        assertEquals(HttpStatus.FORBIDDEN, noHost.getStatusCode());

        lobby.setHostParticipant(requesterParticipant);
        ResponseStatusException notEnoughPlayers = assertThrows(ResponseStatusException.class,
                () -> lobbyService.startGame(1L, "valid-token"));
        assertEquals(HttpStatus.CONFLICT, notEnoughPlayers.getStatusCode());

        LobbyParticipant guestParticipant = participant(101L, user, false);
        lobby.getParticipants().add(guestParticipant);
        lobby.setGameId(99L);
        ResponseStatusException alreadyStarted = assertThrows(ResponseStatusException.class,
                () -> lobbyService.startGame(1L, "valid-token"));
        assertEquals(HttpStatus.CONFLICT, alreadyStarted.getStatusCode());
    }

    @Test
    public void addAndRemoveBot_afterGameStarted_throwConflict() {
        LobbyParticipant hostParticipant = participant(100L, host, false);
        LobbyParticipant botParticipant = participant(101L, null, true);
        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(botParticipant);
        lobby.setHostParticipant(hostParticipant);
        lobby.setGameId(99L);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException addBot = assertThrows(ResponseStatusException.class,
                () -> lobbyService.addBot(1L, "valid-token"));
        ResponseStatusException removeBot = assertThrows(ResponseStatusException.class,
                () -> lobbyService.removeBot(1L, "valid-token", 101L));

        assertEquals(HttpStatus.CONFLICT, addBot.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, removeBot.getStatusCode());
    }

    @Test
    public void removeBot_targetIsHuman_throwsBadRequest() {
        LobbyParticipant hostParticipant = participant(100L, host, false);
        LobbyParticipant guestParticipant = participant(101L, user, false);
        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> lobbyService.removeBot(1L, "valid-token", 101L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void leaveLobby_hostLeaves_transfersHostToRemainingParticipant() {
        LobbyParticipant hostParticipant = participant(100L, host, false);
        LobbyParticipant guestParticipant = participant(101L, user, false);
        lobby.getParticipants().add(hostParticipant);
        lobby.getParticipants().add(guestParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        lobbyService.leaveLobby(1L, "valid-token");

        assertEquals(guestParticipant, lobby.getHostParticipant());
        assertTrue(lobby.getParticipants().contains(guestParticipant));
        assertFalse(lobby.getParticipants().contains(hostParticipant));
        Mockito.verify(lobbyParticipantRepository).delete(hostParticipant);
        Mockito.verify(lobbyRepository).saveAndFlush(lobby);
    }

    @Test
    public void kickAndTransferGuards_throwExpectedErrors() {
        LobbyParticipant hostParticipant = participant(100L, host, false);
        lobby.getParticipants().add(hostParticipant);
        lobby.setHostParticipant(hostParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(host);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));

        ResponseStatusException kickSelf = assertThrows(ResponseStatusException.class,
                () -> lobbyService.kickParticipant(1L, "valid-token", 100L));
        ResponseStatusException kickPlayerMissingUserId = assertThrows(ResponseStatusException.class,
                () -> lobbyService.kickPlayer(1L, "valid-token", null));
        ResponseStatusException transferMissingParticipantId = assertThrows(ResponseStatusException.class,
                () -> lobbyService.transferHost(1L, "valid-token", null));
        ResponseStatusException transferMissingUserId = assertThrows(ResponseStatusException.class,
                () -> lobbyService.transferHostToUser(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, kickSelf.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, kickPlayerMissingUserId.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, transferMissingParticipantId.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, transferMissingUserId.getStatusCode());
    }

    private LobbyParticipant participant(Long id, User user, boolean bot) {
        LobbyParticipant participant = new LobbyParticipant();
        participant.setId(id);
        participant.setLobby(lobby);
        participant.setUser(user);
        participant.setBot(bot);
        participant.setOnline(true);
        participant.setLastSeenAt(Instant.now());
        return participant;
    }
}
