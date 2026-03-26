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

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
	}

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

}
