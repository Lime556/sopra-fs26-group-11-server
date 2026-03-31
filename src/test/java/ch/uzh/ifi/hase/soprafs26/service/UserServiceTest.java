package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

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

        User createdUser = userService.createUser(testUser);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(userRepository, Mockito.times(1)).flush();

        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertEquals(testUser.getPasswordHash(), createdUser.getPasswordHash());
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

}
