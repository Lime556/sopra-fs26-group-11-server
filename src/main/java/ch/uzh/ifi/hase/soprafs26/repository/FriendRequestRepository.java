package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs26.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository("friendRequestRepository")
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findBySenderAndStatus(User sender, FriendRequestStatus status);

    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequestStatus status);

    boolean existsBySenderAndReceiverAndStatus(User sender, User receiver, FriendRequestStatus status);

    boolean existsBySenderAndReceiver(User sender, User receiver);
}