package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs26.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class FriendRequestService {
    
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    public FriendRequestService(
        FriendRequestRepository friendRequestRepository, 
        FriendshipRepository friendshipRepository, 
        UserService userService
    ) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
    }

    public FriendRequest sendFriendRequest(String token, Long receiverId) {
        User sender = userService.authenticate(token);

        if (receiverId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiverId must not be empty.");
        }

        User receiver = userService.getUserById(receiverId);

        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a friend request to yourself.");
        }

        if (friendshipRepository.existsByUserAAndUserB(sender, receiver) || friendshipRepository.existsByUserAAndUserB(receiver, sender)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already friends with this user.");
        }

        if (friendRequestRepository.existsBySenderAndReceiverAndStatus(sender, receiver, FriendRequestStatus.PENDING)
            || friendRequestRepository.existsBySenderAndReceiverAndStatus(receiver, sender, FriendRequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pending friend request already exists between you and this user.");
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequest.setCreatedAt(java.time.Instant.now());

        return friendRequestRepository.saveAndFlush(friendRequest);
    }
}
