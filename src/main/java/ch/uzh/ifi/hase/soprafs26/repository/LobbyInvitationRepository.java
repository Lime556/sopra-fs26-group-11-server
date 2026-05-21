package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyInvitation;
import ch.uzh.ifi.hase.soprafs26.entity.User;

@Repository("lobbyInvitationRepository")
public interface LobbyInvitationRepository extends JpaRepository<LobbyInvitation, Long> {

    boolean existsByLobbyAndReceiverAndStatus(Lobby lobby, User receiver, LobbyInvitationStatus status);

    List<LobbyInvitation> findByReceiverAndStatusOrderByCreatedAtDesc(User receiver, LobbyInvitationStatus status);

    void deleteByLobby(Lobby lobby);
}
