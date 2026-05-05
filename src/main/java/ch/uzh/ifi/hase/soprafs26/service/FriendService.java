package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs26.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class FriendService {
    
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    public FriendService(
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

    public List<FriendRequest> getPendingFriendRequests(String token) {
        User receiver = userService.authenticate(token);
    
        return friendRequestRepository.findByReceiverAndStatus(
            receiver,
            FriendRequestStatus.PENDING
        );
    }

    public FriendRequest acceptFriendRequest(String token, Long requestId) {
        User receiver = userService.authenticate(token);

        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));
        
        if (!friendRequest.getReceiver().getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can accept this friend request.");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Friend request is no longer pending.");
        }

        if (friendshipRepository.existsByUserAAndUserB(friendRequest.getSender(), friendRequest.getReceiver())
            || friendshipRepository.existsByUserAAndUserB(friendRequest.getReceiver(), friendRequest.getSender())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already friends with this user.");
        }

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(friendRequest);

        Friendship friendship = new Friendship();
        friendship.setUserA(friendRequest.getSender());
        friendship.setUserB(friendRequest.getReceiver());
        friendship.setCreatedAt(java.time.Instant.now());

        friendshipRepository.saveAndFlush(friendship);

        return friendRequest;
    }

    public FriendRequest declineFriendRequest(String token, Long requestId) {
        User receiver = userService.authenticate(token);
    
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));
    
        if (!friendRequest.getReceiver().getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can decline this friend request.");
        }
    
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Friend request is no longer pending.");
        }
    
        friendRequest.setStatus(FriendRequestStatus.DECLINED);
    
        return friendRequestRepository.saveAndFlush(friendRequest);
    }

    public List<User> getFriends(String token) {
        User user = userService.authenticate(token);

        List<Friendship> friendships = friendshipRepository.findByUserAOrUserB(user, user);

        List <User> friends = new java.util.ArrayList<>();
        
        for (Friendship friendship: friendships) {
            User friend;

            if (friendship.getUserA().getId().equals(user.getId())) {
                friend = friendship.getUserB();
            } else {
                friend = friendship.getUserA();
            }
            
            friends.add(friend);
        }

        return friends;
    }
}
