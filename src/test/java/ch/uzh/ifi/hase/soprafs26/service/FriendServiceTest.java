package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs26.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;

public class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FriendService friendService;

    private User sender;
    private User receiver;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");
        sender.setEmail("sender@email.com");
        sender.setToken("sender-token");

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");
        receiver.setEmail("receiver@email.com");
        receiver.setToken("receiver-token");

        Mockito.when(userService.authenticate("sender-token")).thenReturn(sender);
        Mockito.when(userService.authenticate("receiver-token")).thenReturn(receiver);

        Mockito.when(userService.getUserById(2L)).thenReturn(receiver);
        Mockito.when(userService.getUserById(1L)).thenReturn(sender);

        Mockito.when(friendRequestRepository.saveAndFlush(Mockito.any(FriendRequest.class)))
            .thenAnswer(invocation -> {
                FriendRequest request = invocation.getArgument(0);
                if (request.getId() == null) {
                    request.setId(100L);
                }
                return request;
            });
    }







    // ============ Send Friend Request Tests ============


    @Test
    public void sendFriendRequest_validInput_createsPendingRequest() {
        FriendRequest result = friendService.sendFriendRequest("sender-token", 2L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(sender, result.getSender());
        assertEquals(receiver, result.getReceiver());
        assertEquals(FriendRequestStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());

        Mockito.verify(userService, Mockito.times(1)).authenticate("sender-token");
        Mockito.verify(userService, Mockito.times(1)).getUserById(2L);
        Mockito.verify(friendRequestRepository, Mockito.times(1))
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }

    @Test
    public void sendFriendRequest_missingReceiverId_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.sendFriendRequest("sender-token", null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("receiverId must not be empty.", exception.getReason());

        Mockito.verify(userService, Mockito.times(1)).authenticate("sender-token");
        Mockito.verify(userService, Mockito.never()).getUserById(Mockito.any());
        Mockito.verify(friendRequestRepository, Mockito.never())
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }

    @Test
    public void sendFriendRequest_toSelf_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.sendFriendRequest("sender-token", 1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("You cannot send a friend request to yourself.", exception.getReason());

        Mockito.verify(userService, Mockito.times(1)).authenticate("sender-token");
        Mockito.verify(userService, Mockito.times(1)).getUserById(1L);
        Mockito.verify(friendRequestRepository, Mockito.never())
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }

    @Test
    public void sendFriendRequest_alreadyFriends_throwsBadRequest() {
        Mockito.when(friendshipRepository.existsByUserAAndUserB(sender, receiver))
            .thenReturn(true);
    
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.sendFriendRequest("sender-token", 2L)
        );
    
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("You are already friends with this user.", exception.getReason());
    
        Mockito.verify(friendRequestRepository, Mockito.never())
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }
    
    @Test
    public void sendFriendRequest_pendingRequestSameDirection_throwsBadRequest() {
        Mockito.when(friendRequestRepository.existsBySenderAndReceiverAndStatus(
            sender,
            receiver,
            FriendRequestStatus.PENDING
        )).thenReturn(true);
    
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.sendFriendRequest("sender-token", 2L)
        );
    
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
            "A pending friend request already exists between you and this user.",
            exception.getReason()
        );
    
        Mockito.verify(friendRequestRepository, Mockito.never())
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }
    
    @Test
    public void sendFriendRequest_pendingRequestReverseDirection_throwsBadRequest() {
        Mockito.when(friendRequestRepository.existsBySenderAndReceiverAndStatus(
            receiver,
            sender,
            FriendRequestStatus.PENDING
        )).thenReturn(true);
    
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.sendFriendRequest("sender-token", 2L)
        );
    
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
            "A pending friend request already exists between you and this user.",
            exception.getReason()
        );
    
        Mockito.verify(friendRequestRepository, Mockito.never())
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }






    // ============ Accept Friend Request Tests ============
    
    @Test
    public void acceptFriendRequest_validInput_acceptsRequestAndCreatesFriendship() {
        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        Mockito.when(friendRequestRepository.findById(100L))
            .thenReturn(Optional.of(request));

        Mockito.when(friendshipRepository.saveAndFlush(Mockito.any(Friendship.class)))
            .thenAnswer(invocation -> {
                Friendship friendship = invocation.getArgument(0);
                if (friendship.getId() == null) {
                    friendship.setId(200L);
                }
                return friendship;
            });

        FriendRequest result = friendService.acceptFriendRequest("receiver-token", 100L);

        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());

        Mockito.verify(friendRequestRepository, Mockito.times(1)).save(request);
        Mockito.verify(friendshipRepository, Mockito.times(1))
            .saveAndFlush(Mockito.any(Friendship.class));
    }

    @Test
    public void acceptFriendRequest_notFound_throwsNotFound() {
        Mockito.when(friendRequestRepository.findById(999L))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.acceptFriendRequest("receiver-token", 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Friend request not found.", exception.getReason());

        Mockito.verify(friendshipRepository, Mockito.never())
            .saveAndFlush(Mockito.any(Friendship.class));
    }

    @Test
    public void acceptFriendRequest_notReceiver_throwsForbidden() {
        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        Mockito.when(friendRequestRepository.findById(100L))
            .thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.acceptFriendRequest("sender-token", 100L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only the receiver can accept this friend request.", exception.getReason());

        Mockito.verify(friendshipRepository, Mockito.never())
            .saveAndFlush(Mockito.any(Friendship.class));
    }






    // ============ Decline Friend Request Tests ============

    @Test
    public void declineFriendRequest_validInput_declinesRequest() {
        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        Mockito.when(friendRequestRepository.findById(100L))
            .thenReturn(Optional.of(request));

        FriendRequest result = friendService.declineFriendRequest("receiver-token", 100L);

        assertEquals(FriendRequestStatus.DECLINED, result.getStatus());

        Mockito.verify(friendRequestRepository, Mockito.times(1))
            .saveAndFlush(request);
    }

    @Test
    public void declineFriendRequest_notFound_throwsNotFound() {
        Mockito.when(friendRequestRepository.findById(999L))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.declineFriendRequest("receiver-token", 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Friend request not found.", exception.getReason());

        Mockito.verify(friendRequestRepository, Mockito.never())
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }

    @Test
    public void declineFriendRequest_notReceiver_throwsForbidden() {
        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        Mockito.when(friendRequestRepository.findById(100L))
            .thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> friendService.declineFriendRequest("sender-token", 100L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only the receiver can decline this friend request.", exception.getReason());

        Mockito.verify(friendRequestRepository, Mockito.never())
            .saveAndFlush(Mockito.any(FriendRequest.class));
    }







    // ============ Get Friends Tests ============

    @Test
    public void getFriends_userIsUserA_returnsUserB() {
        Friendship friendship = new Friendship();
        friendship.setId(200L);
        friendship.setUserA(sender);
        friendship.setUserB(receiver);

        Mockito.when(friendshipRepository.findByUserAOrUserB(sender, sender))
            .thenReturn(List.of(friendship));

        List<User> result = friendService.getFriends("sender-token");

        assertEquals(1, result.size());
        assertEquals(receiver, result.get(0));

        Mockito.verify(userService, Mockito.times(1)).authenticate("sender-token");
        Mockito.verify(friendshipRepository, Mockito.times(1))
            .findByUserAOrUserB(sender, sender);
    }

    @Test
    public void getFriends_userIsUserB_returnsUserA() {
        Friendship friendship = new Friendship();
        friendship.setId(200L);
        friendship.setUserA(sender);
        friendship.setUserB(receiver);

        Mockito.when(friendshipRepository.findByUserAOrUserB(receiver, receiver))
            .thenReturn(List.of(friendship));

        List<User> result = friendService.getFriends("receiver-token");

        assertEquals(1, result.size());
        assertEquals(sender, result.get(0));

        Mockito.verify(userService, Mockito.times(1)).authenticate("receiver-token");
        Mockito.verify(friendshipRepository, Mockito.times(1))
            .findByUserAOrUserB(receiver, receiver);
    }

    @Test
    public void getFriends_noFriendships_returnsEmptyList() {
        Mockito.when(friendshipRepository.findByUserAOrUserB(sender, sender))
            .thenReturn(List.of());

        List<User> result = friendService.getFriends("sender-token");

        assertNotNull(result);
        assertEquals(0, result.size());

        Mockito.verify(userService, Mockito.times(1)).authenticate("sender-token");
        Mockito.verify(friendshipRepository, Mockito.times(1))
            .findByUserAOrUserB(sender, sender);
    }
}
