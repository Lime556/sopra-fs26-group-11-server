package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyInvitation;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyInvitationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;

@Service
@Transactional
public class LobbyInvitationService {

    private final LobbyInvitationRepository lobbyInvitationRepository;
    private final LobbyRepository lobbyRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserService userService;
    private final LobbyService lobbyService;

    public LobbyInvitationService(
        LobbyInvitationRepository lobbyInvitationRepository,
        LobbyRepository lobbyRepository,
        FriendshipRepository friendshipRepository,
        UserService userService,
        LobbyService lobbyService
    ) {
        this.lobbyInvitationRepository = lobbyInvitationRepository;
        this.lobbyRepository = lobbyRepository;
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
        this.lobbyService = lobbyService;
    }

    public LobbyInvitation sendInvitation(String token, Long lobbyId, Long receiverId) {
        User sender = userService.authenticate(token);

        if (receiverId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiverId must not be empty.");
        }

        Lobby lobby = lobbyRepository.findByIdWithLock(lobbyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Lobby with id " + lobbyId + " was not found."));

        if (lobby.getGameId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot invite users after the game has started.");
        }

        LobbyParticipant senderParticipant = lobby.getParticipants().stream()
            .filter(participant -> participant.getUser() != null && participant.getUser().getId().equals(sender.getId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this lobby"));
        if (senderParticipant == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this lobby");
        }

        User receiver = userService.getUserById(receiverId);

        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself.");
        }

        boolean areFriends = friendshipRepository.existsByUserAAndUserB(sender, receiver)
            || friendshipRepository.existsByUserAAndUserB(receiver, sender);
        if (!areFriends) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only invite friends.");
        }

        boolean alreadyInLobby = lobby.getParticipants().stream()
            .anyMatch(participant -> participant.getUser() != null && participant.getUser().getId().equals(receiver.getId()));
        if (alreadyInLobby) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already in this lobby.");
        }

        if (lobbyInvitationRepository.existsByLobbyAndReceiverAndStatus(lobby, receiver, LobbyInvitationStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already has a pending invitation for this lobby.");
        }

        LobbyInvitation invitation = new LobbyInvitation();
        invitation.setLobby(lobby);
        invitation.setSender(sender);
        invitation.setReceiver(receiver);
        invitation.setStatus(LobbyInvitationStatus.PENDING);
        invitation.setCreatedAt(Instant.now());

        return lobbyInvitationRepository.saveAndFlush(invitation);
    }

    public List<LobbyInvitation> getPendingInvitations(String token) {
        User receiver = userService.authenticate(token);
        List<LobbyInvitation> pending = lobbyInvitationRepository.findByReceiverAndStatusOrderByCreatedAtDesc(
            receiver,
            LobbyInvitationStatus.PENDING
        );

        List<LobbyInvitation> validInvitations = new ArrayList<>();
        for (LobbyInvitation invitation : pending) {
            Lobby lobby = invitation.getLobby();
            if (lobby == null || lobby.getGameId() != null) {
                invitation.setStatus(LobbyInvitationStatus.DECLINED);
                lobbyInvitationRepository.save(invitation);
                continue;
            }
            validInvitations.add(invitation);
        }

        return validInvitations;
    }

    public LobbyInvitation acceptInvitation(String token, Long invitationId) {
        User receiver = userService.authenticate(token);

        LobbyInvitation invitation = lobbyInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby invitation not found."));

        if (!invitation.getReceiver().getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invited user can accept this invitation.");
        }

        if (invitation.getStatus() != LobbyInvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby invitation is no longer pending.");
        }

        Lobby lobby = invitation.getLobby();
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby for invitation no longer exists.");
        }

        String lobbyPassword = lobby.getPassword();
        lobbyService.joinLobby(lobby.getId(), token, lobbyPassword);

        invitation.setStatus(LobbyInvitationStatus.ACCEPTED);
        return lobbyInvitationRepository.saveAndFlush(invitation);
    }

    public LobbyInvitation declineInvitation(String token, Long invitationId) {
        User receiver = userService.authenticate(token);

        LobbyInvitation invitation = lobbyInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby invitation not found."));

        if (!invitation.getReceiver().getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invited user can decline this invitation.");
        }

        if (invitation.getStatus() != LobbyInvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby invitation is no longer pending.");
        }

        invitation.setStatus(LobbyInvitationStatus.DECLINED);
        return lobbyInvitationRepository.saveAndFlush(invitation);
    }
}
