package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final GameRepository gameRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository,
			@Qualifier("gameRepository") GameRepository gameRepository) {
		this.userRepository = userRepository;
		this.gameRepository = gameRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User getUserById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		List<Game> allGames = gameRepository.findAll();
		int totalFinished = 0;
		int wins = 0;
		for (Game game : allGames) {
			if (game.getFinishedAt() == null) {
				continue;
			}
			if (game.getPlayers() == null) {
				continue;
			}
			for (Player player : game.getPlayers()) {
				if (player.getUser() != null && id.equals(player.getUser().getId())) {
					totalFinished++;
					if (player.getId() != null && player.getId().equals(game.getWinnerPlayerId())) {
						wins++;
					}
					break;
				}
			}
		}

		double calculatedWinRate = totalFinished > 0 ? (double) wins / totalFinished : 0.0;
		user.setWinRate(calculatedWinRate);
		userRepository.save(user);

		return user;
	}

	public User createUser(User newUser) {
		if (newUser.getUsername() == null || newUser.getUsername().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username must not be empty");
		}

		newUser.setUsername(newUser.getUsername().trim());

		if (newUser.getEmail() == null || newUser.getEmail().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email must not be empty");
		}

		newUser.setEmail(newUser.getEmail().trim());

		if (!EMAIL_PATTERN.matcher(newUser.getEmail()).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email format is invalid");
		}

		if (newUser.getPasswordHash() == null || newUser.getPasswordHash().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must not be empty");
		}

		if (newUser.getPasswordHash().length() < 6) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at least 6 characters");
		}

		checkIfUserExists(newUser);
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setUserStatus(UserStatus.ONLINE);
		newUser.setCreationDate(Instant.now());
		newUser.setWinRate(0.0);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		User userByEmail = userRepository.findByEmail(userToBeCreated.getEmail());

		String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
		}

		if (userByEmail != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "email", "is"));
		}
	}

	public User login(User loginUser) {
		User user = userRepository.findByUsername(loginUser.getUsername());

		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}

		if (!user.getPasswordHash().equals(loginUser.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
		}
		
		user.setToken(UUID.randomUUID().toString());
		user.setUserStatus(UserStatus.ONLINE);
		userRepository.save(user);

		return user;
	}

	public void logout(String token) {
		String extractedToken = extractToken(token);

		if (extractedToken == null || extractedToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
		}

		User user = userRepository.findByToken(extractedToken);

		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
		}

		if (user.getUserStatus() != UserStatus.ONLINE) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
		}

		user.setUserStatus(UserStatus.OFFLINE);
		user.setToken(UUID.randomUUID().toString());

		userRepository.save(user);
	}

	public User authenticate(String token) {
		String extractedToken = extractToken(token);

		if (extractedToken == null || extractedToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
		}
		
		User user = userRepository.findByToken(extractedToken);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
		}

		if (user.getUserStatus() != UserStatus.ONLINE) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
		}

		return user;
	}

	private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length()).trim();
        }
        return authorizationHeader.trim();
    }
}
