package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class LobbyService {

    private static final int DEFAULT_LOBBY_CAPACITY = 4;

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;

    public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository,
                        @Qualifier("userRepository") UserRepository userRepository) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
    }

    public List<Lobby> getLobbies() {
        return lobbyRepository.findAll();
    }

    public Lobby createLobby(String playerToken, Integer capacity, String lobbyPassword) {
        User host = getAuthenticatedUser(playerToken);

        Lobby lobby = new Lobby();
        lobby.setCapacity(resolveCapacity(capacity));
        if (lobbyPassword != null && !lobbyPassword.isBlank()) {
            lobby.setPassword(lobbyPassword.trim());
        }
        lobby.getUsers().add(host);

        Lobby createdLobby = lobbyRepository.save(lobby);
        lobbyRepository.flush();
        return createdLobby;
    }

    public Lobby joinLobby(Long lobbyId, String playerToken, String lobbyPassword) {
        User user = getAuthenticatedUser(playerToken);

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lobby with id " + lobbyId + " was not found."));

        if (lobby.getPassword() != null && !lobby.getPassword().isBlank()) {
            if (lobbyPassword == null || !lobby.getPassword().equals(lobbyPassword)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Wrong lobby password for lobby " + lobbyId + ".");
            }
        }

        boolean alreadyJoined = lobby.getUsers().stream()
                .anyMatch(existingUser -> existingUser.getId().equals(user.getId()));
        if (alreadyJoined) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User with id " + user.getId() + " already joined lobby " + lobbyId + ".");
        }

        if (lobby.getCurrentPlayers() >= lobby.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lobby with id " + lobbyId + " is full.");
        }

        lobby.getUsers().add(user);
        lobby = lobbyRepository.save(lobby);
        lobbyRepository.flush();
        return lobby;
    }

    private User getAuthenticatedUser(String playerToken) {
        if (playerToken == null || playerToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token.");
        }

        User user = userRepository.findByToken(playerToken);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization token.");
        }
        return user;
    }

    private int resolveCapacity(Integer capacity) {
        int resolved = capacity == null ? DEFAULT_LOBBY_CAPACITY : capacity;
        if (resolved < 2 || resolved > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby capacity must be between 2 and 4.");
        }
        return resolved;
    }
}
