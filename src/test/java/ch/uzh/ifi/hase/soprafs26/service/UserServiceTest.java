package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameHistoryEntryDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PasswordUpdateDTO;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

	@Mock
	private UserRepository userRepository;

	@Mock
	private GameRepository gameRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(1L);
		testUser.setUsername("testUsername");
        testUser.setPasswordHash("testPassword");
		testUser.setEmail("test@email.com");

		// mock repository save to return the saved user
		Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
	}



	// User creation tests
    @Test
    public void createUser_validInputs_success() {
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
        String rawPassword = testUser.getPasswordHash();

        User createdUser = userService.createUser(testUser);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(userRepository, Mockito.times(1)).flush();

        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotEquals(rawPassword, createdUser.getPasswordHash());
        assertTrue(PASSWORD_ENCODER.matches(rawPassword, createdUser.getPasswordHash()));
        assertNotNull(createdUser.getToken());
        assertNotNull(createdUser.getCreationDate());
        assertEquals(UserStatus.ONLINE, createdUser.getUserStatus());
        assertEquals(0.0, createdUser.getWinRate());
    }


    @Test
    public void createUser_duplicateUsername_throwsException() {
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_emptyUsername_throwsException() {
        User invalidUser = new User();
        invalidUser.setUsername("");
        invalidUser.setPasswordHash("testPassword");

        assertThrows(ResponseStatusException.class, () -> userService.createUser(invalidUser));
    }

    @Test
    public void createUser_emptyPassword_throwsException() {
        User invalidUser = new User();
        invalidUser.setUsername("testUsername");
        invalidUser.setPasswordHash("");

        assertThrows(ResponseStatusException.class, () -> userService.createUser(invalidUser));
    }

	@Test
	public void createUser_usernameWithLeadingTrailingSpaces_trimmed() {
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

		User user = new User();
		user.setUsername("  testUsername  ");
		user.setEmail("test@email.com");
		user.setPasswordHash("testPassword");

		User createdUser = userService.createUser(user);

		assertEquals("testUsername", createdUser.getUsername());
	}

	@Test
	public void createUser_nullUsername_throwsException() {
		User invalidUser = new User();
		invalidUser.setUsername(null);
		invalidUser.setEmail("test@email.com");
		invalidUser.setPasswordHash("testPassword");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(invalidUser));
	}




	// Authentication tests
	@Test
	public void authenticate_validToken_success() {
		User onlineUser = new User();
		onlineUser.setId(1L);
		onlineUser.setUsername("testUsername");
		onlineUser.setEmail("test@email.com");
		onlineUser.setToken("valid-token");
		onlineUser.setUserStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(onlineUser);

		User authenticatedUser = userService.authenticate("valid-token");

		assertEquals(onlineUser.getId(), authenticatedUser.getId());
		assertEquals(onlineUser.getUsername(), authenticatedUser.getUsername());
		assertEquals(onlineUser.getToken(), authenticatedUser.getToken());
	}

	@Test
	public void authenticate_nullToken_throwsException() {
		assertThrows(ResponseStatusException.class, () -> userService.authenticate(null));
	}

	@Test
	public void authenticate_blankToken_throwsException() {
		assertThrows(ResponseStatusException.class, () -> userService.authenticate("   "));
	}

	@Test
	public void authenticate_unknownToken_throwsException() {
		Mockito.when(userRepository.findByToken("unknown-token")).thenReturn(null);

		assertThrows(ResponseStatusException.class, () -> userService.authenticate("unknown-token"));
	}

	@Test
	public void authenticate_offlineUser_throwsException() {
		User offlineUser = new User();
		offlineUser.setId(1L);
		offlineUser.setUsername("testUsername");
		offlineUser.setEmail("test@email.com");
		offlineUser.setToken("offline-token");
		offlineUser.setUserStatus(UserStatus.OFFLINE);

		Mockito.when(userRepository.findByToken("offline-token")).thenReturn(offlineUser);

		assertThrows(ResponseStatusException.class, () -> userService.authenticate("offline-token"));
	}

	@Test
	public void createUser_withEmail_success() {
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

		User user = new User();
		user.setUsername("testUser");
		user.setPasswordHash("password");
		user.setEmail("test@email.com");

		User createdUser = userService.createUser(user);

		assertEquals("test@email.com", createdUser.getEmail());
	}

	
	// Login tests
	@Test
	public void login_validCredentials_success() {
		User existingUser = new User();
		existingUser.setId(1L);
		existingUser.setUsername("testUsername");
		existingUser.setPasswordHash(PASSWORD_ENCODER.encode("testPassword"));
		existingUser.setEmail("test@email.com");

		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(existingUser);

		User loginUser = new User();
		loginUser.setUsername("testUsername");
		loginUser.setPasswordHash("testPassword");

		User result = userService.login(loginUser);

		assertNotNull(result.getToken());
		assertEquals(existingUser.getUsername(), result.getUsername());
		assertEquals(UserStatus.ONLINE, result.getUserStatus());
		Mockito.verify(userRepository, Mockito.times(1)).save(existingUser);
	}

	@Test
	public void login_wrongPassword_throwsUnauthorized() {
		User existingUser = new User();
		existingUser.setUsername("testUsername");
		existingUser.setPasswordHash(PASSWORD_ENCODER.encode("password"));

		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(existingUser);

		User loginUser = new User();
		loginUser.setUsername("testUsername");
		loginUser.setPasswordHash("wrongPassword");

		assertThrows(ResponseStatusException.class, () -> userService.login(loginUser));
	}

	@Test
	public void login_legacyPlaintextPassword_success() {
		User existingUser = new User();
		existingUser.setId(1L);
		existingUser.setUsername("testUsername");
		existingUser.setPasswordHash("testPassword");
		existingUser.setEmail("test@email.com");

		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(existingUser);

		User loginUser = new User();
		loginUser.setUsername("testUsername");
		loginUser.setPasswordHash("testPassword");

		User result = userService.login(loginUser);

		assertNotNull(result.getToken());
		assertEquals(UserStatus.ONLINE, result.getUserStatus());
		Mockito.verify(userRepository, Mockito.times(1)).save(existingUser);
	}

	@Test
	public void login_unknownUser_throwsNotFound() {
		Mockito.when(userRepository.findByUsername("unknownUser")).thenReturn(null);

		User loginUser = new User();
		loginUser.setUsername("unknownUser");
		loginUser.setPasswordHash("anyPassword");

		assertThrows(ResponseStatusException.class, () -> userService.login(loginUser));
	}

	// Password update tests
	@Test
	public void updatePassword_validInput_updatesPasswordWithBCryptHash() {
		User requester = new User();
		requester.setId(1L);
		requester.setUsername("testUsername");
		requester.setToken("valid-token");
		requester.setUserStatus(UserStatus.ONLINE);
		requester.setPasswordHash(PASSWORD_ENCODER.encode("oldPassword"));

		PasswordUpdateDTO dto = new PasswordUpdateDTO();
		dto.setCurrentPassword("oldPassword");
		dto.setNewPassword("newPassword");

		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(requester);

		userService.updatePassword(1L, "valid-token", dto);

		assertNotEquals("newPassword", requester.getPasswordHash());
		assertTrue(PASSWORD_ENCODER.matches("newPassword", requester.getPasswordHash()));
		Mockito.verify(userRepository, Mockito.times(1)).save(requester);
	}

	@Test
	public void updatePassword_legacyPlaintextCurrentPassword_rehashesNewPassword() {
		User requester = new User();
		requester.setId(1L);
		requester.setUsername("testUsername");
		requester.setToken("valid-token");
		requester.setUserStatus(UserStatus.ONLINE);
		requester.setPasswordHash("oldPassword");

		PasswordUpdateDTO dto = new PasswordUpdateDTO();
		dto.setCurrentPassword("oldPassword");
		dto.setNewPassword("newPassword");

		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(requester);

		userService.updatePassword(1L, "valid-token", dto);

		assertNotEquals("newPassword", requester.getPasswordHash());
		assertTrue(PASSWORD_ENCODER.matches("newPassword", requester.getPasswordHash()));
		Mockito.verify(userRepository, Mockito.times(1)).save(requester);
	}

	@Test
	public void updatePassword_wrongCurrentPassword_throwsUnauthorized() {
		User requester = new User();
		requester.setId(1L);
		requester.setUsername("testUsername");
		requester.setToken("valid-token");
		requester.setUserStatus(UserStatus.ONLINE);
		requester.setPasswordHash(PASSWORD_ENCODER.encode("oldPassword"));

		PasswordUpdateDTO dto = new PasswordUpdateDTO();
		dto.setCurrentPassword("wrongPassword");
		dto.setNewPassword("newPassword");

		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(requester);

		assertThrows(ResponseStatusException.class, () -> userService.updatePassword(1L, "valid-token", dto));
		Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
	}


	// Logout tests
	@Test
	public void logout_validToken_success() {
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setToken("valid-token");
		user.setUserStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);

		userService.logout("valid-token");

		assertEquals(UserStatus.OFFLINE, user.getUserStatus());
		assertNotEquals("valid-token", user.getToken());
		Mockito.verify(userRepository, Mockito.times(1)).save(user);
	}
	
	@Test
	public void logout_invalidToken_throwsException() {
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setToken("valid-token");
		user.setUserStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);

		assertThrows(ResponseStatusException.class, () -> userService.logout("invalid-token"));
	}

	// Profile and history tests
	@Test
	public void getUserById_success_returnsUser() {
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

		User result = userService.getUserById(1L);

		assertEquals(testUser, result);
	}

	@Test
	public void getUserById_missingUser_throwsNotFound() {
		Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> userService.getUserById(999L));
	}

	@Test
	public void getUserByIdWithWinRate_calculatesAndSavesWinRate() {
		User profileUser = new User();
		profileUser.setId(1L);
		profileUser.setUsername("testUsername");
		profileUser.setPasswordHash("testPassword");
		profileUser.setEmail("test@email.com");
		profileUser.setUserStatus(UserStatus.ONLINE);
		profileUser.setWinRate(0.0);

		User opponentUser = new User();
		opponentUser.setId(2L);
		opponentUser.setUsername("opponent");
		opponentUser.setPasswordHash("opponentPassword");
		opponentUser.setEmail("opponent@email.com");
		opponentUser.setUserStatus(UserStatus.ONLINE);

		Game wonGame = createGame(
			1L,
			LocalDateTime.of(2026, 1, 1, 10, 0),
			LocalDateTime.of(2026, 1, 1, 11, 0),
			11L,
			createPlayer(11L, profileUser, 9, 1L),
			createPlayer(12L, opponentUser, 5, 1L)
		);

		Game lostGame = createGame(
			2L,
			LocalDateTime.of(2026, 1, 2, 10, 0),
			LocalDateTime.of(2026, 1, 2, 11, 0),
			22L,
			createPlayer(21L, profileUser, 7, 2L),
			createPlayer(22L, opponentUser, 10, 2L)
		);

		Game unfinishedGame = createGame(
			3L,
			LocalDateTime.of(2026, 1, 3, 10, 0),
			null,
			31L,
			createPlayer(31L, profileUser, 6, 3L),
			createPlayer(32L, opponentUser, 4, 3L)
		);

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(profileUser));
		Mockito.when(gameRepository.findAll()).thenReturn(List.of(wonGame, lostGame, unfinishedGame));

		User result = userService.getUserByIdWithWinRate(1L);

		assertEquals(0.5, result.getWinRate());
		Mockito.verify(userRepository, Mockito.times(1)).save(profileUser);
	}

	@Test
	public void getGameHistory_returnsSortedLastTenGames() {
		User profileUser = new User();
		profileUser.setId(1L);
		profileUser.setUsername("testUsername");
		profileUser.setPasswordHash("testPassword");
		profileUser.setEmail("test@email.com");
		profileUser.setUserStatus(UserStatus.ONLINE);

		User opponentUser = new User();
		opponentUser.setId(2L);
		opponentUser.setUsername("opponent");
		opponentUser.setPasswordHash("opponentPassword");
		opponentUser.setEmail("opponent@email.com");
		opponentUser.setUserStatus(UserStatus.ONLINE);

		List<Game> games = new ArrayList<>();
		for (int i = 1; i <= 11; i++) {
			boolean userWon = i % 2 == 1;
			games.add(createGame(
				(long) i,
				LocalDateTime.of(2026, 1, i, 10, 0),
				LocalDateTime.of(2026, 1, i, 11, 0),
				userWon ? (long) (i * 10 + 1) : (long) (i * 10 + 2),
				createPlayer((long) (i * 10 + 1), profileUser, userWon ? 10 : 6, (long) i),
				createPlayer((long) (i * 10 + 2), opponentUser, userWon ? 6 : 10, (long) i)
			));
		}

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(profileUser));
		Mockito.when(gameRepository.findAll()).thenReturn(games);

		List<GameHistoryEntryDTO> result = userService.getGameHistory(1L);

		assertEquals(10, result.size());
		assertEquals(11L, result.get(0).getGameId());
		assertEquals(2L, result.get(9).getGameId());
		assertTrue(result.get(0).isWon());
		assertEquals(10, result.get(0).getPlayerVictoryPoints());
		assertTrue(result.get(0).getPlayerNames().contains("testUsername"));
		assertTrue(result.get(0).getPlayerNames().contains("opponent"));
	}

	@Test
	public void getGameHistory_missingUser_throwsNotFound() {
		Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> userService.getGameHistory(999L));
	}

	@Test
	public void updatePassword_validInput_updatesPassword() {
		User requester = new User();
		requester.setId(1L);
		requester.setUsername("testUsername");
		requester.setPasswordHash("oldPassword");
		requester.setEmail("test@email.com");
		requester.setUserStatus(UserStatus.ONLINE);

		PasswordUpdateDTO dto = new PasswordUpdateDTO();
		dto.setCurrentPassword("oldPassword");
		dto.setNewPassword("newPassword");

		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(requester);

		userService.updatePassword(1L, "valid-token", dto);

		assertTrue(PASSWORD_ENCODER.matches("newPassword", requester.getPasswordHash()));
		Mockito.verify(userRepository, Mockito.times(1)).save(requester);
	}

	@Test
	public void updatePassword_otherUser_throwsForbidden() {
		User requester = new User();
		requester.setId(2L);
		requester.setUsername("otherUser");
		requester.setPasswordHash("oldPassword");
		requester.setEmail("other@email.com");
		requester.setUserStatus(UserStatus.ONLINE);

		PasswordUpdateDTO dto = new PasswordUpdateDTO();
		dto.setCurrentPassword("oldPassword");
		dto.setNewPassword("newPassword");

		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(requester);

		assertThrows(ResponseStatusException.class, () -> userService.updatePassword(1L, "valid-token", dto));
	}

	private Game createGame(Long gameId, LocalDateTime startedAt, LocalDateTime finishedAt,
			Long winnerPlayerId, Player... players) {
		Game game = new Game();
		game.setId(gameId);
		game.setStartedAt(startedAt);
		game.setFinishedAt(finishedAt);
		game.setWinnerPlayerId(winnerPlayerId);
		game.setPlayers(List.of(players));
		return game;
	}

	private Player createPlayer(Long playerId, User user, int victoryPoints, Long gameId) {
		Player player = new Player();
		player.setId(playerId);
		player.setUser(user);
		player.setVictoryPoints(victoryPoints);
		player.setGameId(gameId);
		player.setColor("red");
		player.setBot(false);
		player.setName(user.getUsername());
		return player;
	}
}
