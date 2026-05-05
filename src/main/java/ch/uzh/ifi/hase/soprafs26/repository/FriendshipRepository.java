package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository("friendshipRepository")
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserAOrUserB(User userA, User userB);

    boolean existsByUserAAndUserB(User userA, User userB);
}