package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.List;

import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LobbyParticipantRepository extends JpaRepository<LobbyParticipant, Long> {
    List<LobbyParticipant> findByUser_Id(Long userId);
}
