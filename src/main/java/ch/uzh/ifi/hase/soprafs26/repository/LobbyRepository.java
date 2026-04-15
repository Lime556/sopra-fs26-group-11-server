package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import jakarta.persistence.LockModeType;

@Repository("lobbyRepository")
public interface LobbyRepository extends JpaRepository<Lobby, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lobby l WHERE l.id = :lobbyId")
    Optional<Lobby> findByIdWithLock(@Param("lobbyId") Long lobbyId);
}
