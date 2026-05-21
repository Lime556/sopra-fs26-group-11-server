package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyInvitation;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyInvitationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;

class LobbyInvitationServiceTest {

    @Mock
    private LobbyInvitationRepository lobbyInvitationRepository;

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserService userService;

    @Mock
    private LobbyService lobbyService;

    @InjectMocks
    private LobbyInvitationService lobbyInvitationService;

    private User sender;
    private User receiver;
    private Lobby lobby;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");

        lobby = new Lobby();
        lobby.setId(7L);
        lobby.setName("Invite Lobby");

        LobbyParticipant senderParticipant = new LobbyParticipant();
        senderParticipant.setId(10L);
        senderParticipant.setLobby(lobby);
        senderParticipant.setUser(sender);

        lobby.setParticipants(Set.of(senderParticipant));

        Mockito.when(userService.authenticate("sender-token")).thenReturn(sender);
        Mockito.when(userService.authenticate("receiver-token")).thenReturn(receiver);
        Mockito.when(userService.getUserById(2L)).thenReturn(receiver);
        Mockito.when(lobbyRepository.findByIdWithLock(7L)).thenReturn(Optional.of(lobby));

        Mockito.when(lobbyInvitationRepository.saveAndFlush(Mockito.any(LobbyInvitation.class)))
            .thenAnswer(invocation -> {
                LobbyInvitation invitation = invocation.getArgument(0);
                if (invitation.getId() == null) {
                    invitation.setId(100L);
                }
                return invitation;
            });
    }

    @Test
    void sendInvitation_validInput_createsPendingInvitation() {
        Mockito.when(friendshipRepository.existsByUserAAndUserB(sender, receiver)).thenReturn(true);

        LobbyInvitation result = lobbyInvitationService.sendInvitation("sender-token", 7L, 2L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(lobby, result.getLobby());
        assertEquals(sender, result.getSender());
        assertEquals(receiver, result.getReceiver());
        assertEquals(LobbyInvitationStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());

        Mockito.verify(lobbyInvitationRepository, Mockito.times(1)).saveAndFlush(Mockito.any(LobbyInvitation.class));
    }

    @Test
    void sendInvitation_missingReceiver_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 7L, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("receiverId must not be empty.", exception.getReason());
    }

    @Test
    void sendInvitation_lobbyNotFound_throwsNotFound() {
        Mockito.when(lobbyRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 999L, 2L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void sendInvitation_gameAlreadyStarted_throwsConflict() {
        lobby.setGameId(5L);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 7L, 2L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Cannot invite users after the game has started.", exception.getReason());
    }

    @Test
    void sendInvitation_senderNotInLobby_throwsForbidden() {
        lobby.setParticipants(Set.of());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 7L, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("User is not part of this lobby", exception.getReason());
    }

    @Test
    void sendInvitation_inviteSelf_throwsBadRequest() {
        Mockito.when(userService.getUserById(1L)).thenReturn(sender);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 7L, 1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("You cannot invite yourself.", exception.getReason());
    }

    @Test
    void sendInvitation_notFriends_throwsForbidden() {
        Mockito.when(friendshipRepository.existsByUserAAndUserB(sender, receiver)).thenReturn(false);
        Mockito.when(friendshipRepository.existsByUserAAndUserB(receiver, sender)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 7L, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You can only invite friends.", exception.getReason());
    }

    @Test
    void sendInvitation_receiverAlreadyInLobby_throwsBadRequest() {
        LobbyParticipant receiverParticipant = new LobbyParticipant();
        receiverParticipant.setId(11L);
        receiverParticipant.setLobby(lobby);
        receiverParticipant.setUser(receiver);

        LobbyParticipant senderParticipant = new LobbyParticipant();
        senderParticipant.setId(10L);
        senderParticipant.setLobby(lobby);
        senderParticipant.setUser(sender);

        lobby.setParticipants(Set.of(senderParticipant, receiverParticipant));
        Mockito.when(friendshipRepository.existsByUserAAndUserB(sender, receiver)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 7L, 2L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("User is already in this lobby.", exception.getReason());
    }

    @Test
    void sendInvitation_pendingExists_throwsBadRequest() {
        Mockito.when(friendshipRepository.existsByUserAAndUserB(sender, receiver)).thenReturn(true);
        Mockito.when(lobbyInvitationRepository.existsByLobbyAndReceiverAndStatus(
            lobby,
            receiver,
            LobbyInvitationStatus.PENDING
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.sendInvitation("sender-token", 7L, 2L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("User already has a pending invitation for this lobby.", exception.getReason());
    }

    @Test
    void getPendingInvitations_filtersOutInvalidLobbies() {
        Lobby validLobby = new Lobby();
        validLobby.setId(1L);
        validLobby.setGameId(null);

        Lobby startedLobby = new Lobby();
        startedLobby.setId(2L);
        startedLobby.setGameId(22L);

        LobbyInvitation validInvitation = new LobbyInvitation();
        validInvitation.setId(101L);
        validInvitation.setLobby(validLobby);
        validInvitation.setStatus(LobbyInvitationStatus.PENDING);

        LobbyInvitation startedLobbyInvitation = new LobbyInvitation();
        startedLobbyInvitation.setId(102L);
        startedLobbyInvitation.setLobby(startedLobby);
        startedLobbyInvitation.setStatus(LobbyInvitationStatus.PENDING);

        LobbyInvitation nullLobbyInvitation = new LobbyInvitation();
        nullLobbyInvitation.setId(103L);
        nullLobbyInvitation.setLobby(null);
        nullLobbyInvitation.setStatus(LobbyInvitationStatus.PENDING);

        Mockito.when(lobbyInvitationRepository.findByReceiverAndStatusOrderByCreatedAtDesc(
            receiver,
            LobbyInvitationStatus.PENDING
        )).thenReturn(List.of(validInvitation, startedLobbyInvitation, nullLobbyInvitation));

        List<LobbyInvitation> result = lobbyInvitationService.getPendingInvitations("receiver-token");

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
        assertEquals(LobbyInvitationStatus.DECLINED, startedLobbyInvitation.getStatus());
        assertEquals(LobbyInvitationStatus.DECLINED, nullLobbyInvitation.getStatus());

        Mockito.verify(lobbyInvitationRepository, Mockito.times(2)).save(Mockito.any(LobbyInvitation.class));
    }

    @Test
    void acceptInvitation_validInput_joinsLobbyAndMarksAccepted() {
        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(111L);
        invitation.setLobby(lobby);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.PENDING);

        lobby.setPassword("secret");

        Mockito.when(lobbyInvitationRepository.findById(111L)).thenReturn(Optional.of(invitation));

        LobbyInvitation result = lobbyInvitationService.acceptInvitation("receiver-token", 111L);

        assertEquals(LobbyInvitationStatus.ACCEPTED, result.getStatus());
        Mockito.verify(lobbyService, Mockito.times(1)).joinLobby(lobby.getId(), "receiver-token", "secret");
        Mockito.verify(lobbyInvitationRepository, Mockito.times(1)).saveAndFlush(invitation);
    }

    @Test
    void acceptInvitation_notFound_throwsNotFound() {
        Mockito.when(lobbyInvitationRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.acceptInvitation("receiver-token", 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Lobby invitation not found.", exception.getReason());
    }

    @Test
    void acceptInvitation_notReceiver_throwsForbidden() {
        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(111L);
        invitation.setLobby(lobby);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.PENDING);

        Mockito.when(lobbyInvitationRepository.findById(111L)).thenReturn(Optional.of(invitation));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.acceptInvitation("sender-token", 111L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only the invited user can accept this invitation.", exception.getReason());
    }

    @Test
    void acceptInvitation_notPending_throwsBadRequest() {
        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(111L);
        invitation.setLobby(lobby);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.DECLINED);

        Mockito.when(lobbyInvitationRepository.findById(111L)).thenReturn(Optional.of(invitation));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.acceptInvitation("receiver-token", 111L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Lobby invitation is no longer pending.", exception.getReason());
    }

    @Test
    void acceptInvitation_lobbyMissing_throwsNotFound() {
        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(111L);
        invitation.setLobby(null);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.PENDING);

        Mockito.when(lobbyInvitationRepository.findById(111L)).thenReturn(Optional.of(invitation));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.acceptInvitation("receiver-token", 111L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Lobby for invitation no longer exists.", exception.getReason());
    }

    @Test
    void declineInvitation_validInput_setsDeclined() {
        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(222L);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.PENDING);
        invitation.setCreatedAt(Instant.parse("2026-05-21T10:00:00Z"));

        Mockito.when(lobbyInvitationRepository.findById(222L)).thenReturn(Optional.of(invitation));

        LobbyInvitation result = lobbyInvitationService.declineInvitation("receiver-token", 222L);

        assertEquals(LobbyInvitationStatus.DECLINED, result.getStatus());
        assertEquals(Instant.parse("2026-05-21T10:00:00Z"), result.getCreatedAt());
        Mockito.verify(lobbyInvitationRepository, Mockito.times(1)).saveAndFlush(invitation);
    }

    @Test
    void declineInvitation_notFound_throwsNotFound() {
        Mockito.when(lobbyInvitationRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.declineInvitation("receiver-token", 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Lobby invitation not found.", exception.getReason());
    }

    @Test
    void declineInvitation_notReceiver_throwsForbidden() {
        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(222L);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.PENDING);

        Mockito.when(lobbyInvitationRepository.findById(222L)).thenReturn(Optional.of(invitation));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.declineInvitation("sender-token", 222L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only the invited user can decline this invitation.", exception.getReason());
    }

    @Test
    void declineInvitation_notPending_throwsBadRequest() {
        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setId(222L);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.ACCEPTED);

        Mockito.when(lobbyInvitationRepository.findById(222L)).thenReturn(Optional.of(invitation));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> lobbyInvitationService.declineInvitation("receiver-token", 222L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Lobby invitation is no longer pending.", exception.getReason());
    }
}
