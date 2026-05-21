package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyInvitation;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyInvitationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyParticipantRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

@SpringBootTest
class LobbyInviteCloseIntegrationTest {

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private LobbyInvitationService lobbyInvitationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private LobbyParticipantRepository lobbyParticipantRepository;

    @Autowired
    private LobbyInvitationRepository lobbyInvitationRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @MockitoBean
    private UserService userService;

    private User host;
    private User invitedFriend;

    @BeforeEach
    void setup() {
        lobbyInvitationRepository.deleteAll();
        lobbyParticipantRepository.deleteAll();
        lobbyRepository.deleteAll();
        friendshipRepository.deleteAll();
        playerRepository.deleteAll();
        gameRepository.deleteAll();
        userRepository.deleteAll();

        host = createUser("host", "host-token", "host@example.com");
        invitedFriend = createUser("friend", "friend-token", "friend@example.com");

        Friendship friendship = new Friendship();
        friendship.setUserA(host);
        friendship.setUserB(invitedFriend);
        friendship.setCreatedAt(Instant.now());
        friendshipRepository.saveAndFlush(friendship);

        given(userService.authenticate("host-token")).willReturn(host);
        given(userService.getUserById(invitedFriend.getId())).willReturn(invitedFriend);
    }

    @Test
    void inviteFriend_thenCloseLobby_lobbyAndInvitesAreDeleted() {
        Lobby lobby = lobbyService.createLobby("host-token", 4, null, "Invite Close Lobby");

        LobbyInvitation invitation = lobbyInvitationService.sendInvitation(
            "host-token",
            lobby.getId(),
            invitedFriend.getId()
        );

        assertTrue(lobbyRepository.findById(lobby.getId()).isPresent());
        assertEquals(1L, lobbyInvitationRepository.count());
        assertEquals(LobbyInvitationStatus.PENDING, invitation.getStatus());

        assertDoesNotThrow(() -> lobbyService.closeLobby(lobby.getId(), "host-token"));

        assertFalse(lobbyRepository.findById(lobby.getId()).isPresent());
        assertTrue(lobbyInvitationRepository.findAll().isEmpty());
    }

    private User createUser(String username, String token, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("pw");
        user.setToken(token);
        user.setEmail(email);
        user.setCreationDate(Instant.now());
        user.setUserStatus(UserStatus.ONLINE);
        user.setWinRate(0.0);
        return userRepository.saveAndFlush(user);
    }
}