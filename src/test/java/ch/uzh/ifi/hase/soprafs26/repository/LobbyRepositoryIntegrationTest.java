package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;

@DataJpaTest
public class LobbyRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Test
    public void saveLobby_persistsAndGeneratesId() {
        Lobby lobby = new Lobby();
        lobby.setName("Test Lobby");
        lobby.setCapacity(4);

        Lobby persistedLobby = lobbyRepository.save(lobby);
        entityManager.flush();

        assertNotNull(persistedLobby.getId());
    }

    @Test
    public void findByIdWithLock_existingLobby_returnsLobby() {
        Lobby lobby = new Lobby();
        lobby.setName("Locked Lobby");
        lobby.setCapacity(3);
        entityManager.persistAndFlush(lobby);

        Optional<Lobby> result = lobbyRepository.findByIdWithLock(lobby.getId());

        assertTrue(result.isPresent());
        assertEquals("Locked Lobby", result.get().getName());
        assertEquals(3, result.get().getCapacity());
    }

    @Test
    public void findByIdWithLock_nonExistingId_returnsEmpty() {
        Optional<Lobby> result = lobbyRepository.findByIdWithLock(999L);

        assertTrue(result.isEmpty());
    }
}
