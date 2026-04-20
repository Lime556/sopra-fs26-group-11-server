package ch.uzh.ifi.hase.soprafs26.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void getLobbies_returnsAll() {
        Lobby first = new Lobby();
        first.setId(1L);
        Lobby second = new Lobby();
        second.setId(2L);
        Mockito.when(lobbyRepository.findAll()).thenReturn(List.of(first, second));

        List<Lobby> lobbies = lobbyService.getLobbies();

        assertEquals(2, lobbies.size());
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
    public void joinLobby_userAlreadyInLobby_throwsConflict() {
        LobbyParticipant existingParticipant = new LobbyParticipant();
        existingParticipant.setId(100L);
        existingParticipant.setUser(user);
        existingParticipant.setLobby(lobby);
        existingParticipant.setBot(false);

        lobby.getParticipants().add(existingParticipant);

        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lobby));


        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lobbyService.joinLobby(1L, "valid-token", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void getLobbyById_validId_success() {
        Mockito.when(userService.authenticate("valid-token")).thenReturn(user);
        Mockito.when(lobbyRepository.findById(1L)).thenReturn(Optional.of(lobby));

        Lobby result = lobbyService.getLobbyById(1L, "valid-token");

        assertEquals(1L, result.getId());
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
        assertEquals(game.getId(), lobby.getGameId());
    }
}
