package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class LobbyRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Test
    public void saveLobby_persistsAndGeneratesId() {
        Lobby lobby = new Lobby();
        lobby.setCapacity(4);

        Lobby persistedLobby = lobbyRepository.save(lobby);
        entityManager.flush();

        assertNotNull(persistedLobby.getId());
    }
}
