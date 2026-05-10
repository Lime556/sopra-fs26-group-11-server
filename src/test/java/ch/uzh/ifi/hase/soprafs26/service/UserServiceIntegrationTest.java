package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

	@Qualifier("userRepository")
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@BeforeEach
	public void setup() {
		userRepository.deleteAll();
	}

	@Test
	public void createUser_validInputs_success() {
		// given
		assertNull(userRepository.findByUsername("testUsername"));
	
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@email.com");
		testUser.setPasswordHash("testPassword");
		String rawPassword = testUser.getPasswordHash();
	
		// when
		User createdUser = userService.createUser(testUser);
	
		// then
		assertNotNull(createdUser.getId());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getPasswordHash());
		org.junit.jupiter.api.Assertions.assertNotEquals(rawPassword, createdUser.getPasswordHash());
		org.junit.jupiter.api.Assertions.assertTrue(PASSWORD_ENCODER.matches(rawPassword, createdUser.getPasswordHash()));
		assertNotNull(createdUser.getToken());
		assertNotNull(createdUser.getCreationDate());
		assertEquals(UserStatus.ONLINE, createdUser.getUserStatus());
		assertEquals(0.0, createdUser.getWinRate());
	}

	@Test
	public void createUser_duplicateUsername_throwsException() {
		// given
		assertNull(userRepository.findByUsername("testUsername"));
	
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@email.com");
		testUser.setPasswordHash("testPassword");
		userService.createUser(testUser);
	
		// attempt to create second user with same username
		User testUser2 = new User();
		testUser2.setUsername("testUsername");
		testUser2.setEmail("another@email.com");
		testUser2.setPasswordHash("anotherPassword");
	
		// then
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
	}
	
	@Test
	public void login_validCredentials_success() {
		// given
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@email.com");
		testUser.setPasswordHash("testPassword");
		User createdUser = userService.createUser(testUser);

		User loginUser = new User();
		loginUser.setUsername("testUsername");
		loginUser.setPasswordHash("testPassword");

		// when
		User loggedIn = userService.login(loginUser);

		// then
		assertEquals(createdUser.getId(), loggedIn.getId());
		assertNotNull(loggedIn.getToken());
		assertEquals(UserStatus.ONLINE, loggedIn.getUserStatus());
		assertEquals(createdUser.getUsername(), loggedIn.getUsername());
	}
}
