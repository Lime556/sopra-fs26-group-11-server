package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.LobbyInvitation;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyInvitationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyInvitationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyInvitationService;

@RestController
public class LobbyInvitationController {

    private final LobbyInvitationService lobbyInvitationService;

    public LobbyInvitationController(LobbyInvitationService lobbyInvitationService) {
        this.lobbyInvitationService = lobbyInvitationService;
    }

    @PostMapping("/lobbies/{lobbyId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyInvitationGetDTO sendLobbyInvitation(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long lobbyId,
        @RequestBody LobbyInvitationPostDTO lobbyInvitationPostDTO
    ) {
        LobbyInvitation invitation = lobbyInvitationService.sendInvitation(
            token,
            lobbyId,
            lobbyInvitationPostDTO == null ? null : lobbyInvitationPostDTO.getReceiverId()
        );

        return DTOMapper.INSTANCE.convertEntityToLobbyInvitationGetDTO(invitation);
    }

    @GetMapping("/lobby-invitations")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LobbyInvitationGetDTO> getPendingLobbyInvitations(
        @RequestHeader(value = "Authorization", required = false) String token
    ) {
        List<LobbyInvitation> invitations = lobbyInvitationService.getPendingInvitations(token);
        List<LobbyInvitationGetDTO> invitationDTOs = new ArrayList<>();

        for (LobbyInvitation invitation : invitations) {
            invitationDTOs.add(DTOMapper.INSTANCE.convertEntityToLobbyInvitationGetDTO(invitation));
        }

        return invitationDTOs;
    }

    @PutMapping("/lobby-invitations/{invitationId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyInvitationGetDTO acceptLobbyInvitation(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long invitationId
    ) {
        LobbyInvitation invitation = lobbyInvitationService.acceptInvitation(token, invitationId);
        return DTOMapper.INSTANCE.convertEntityToLobbyInvitationGetDTO(invitation);
    }

    @PutMapping("/lobby-invitations/{invitationId}/decline")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyInvitationGetDTO declineLobbyInvitation(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long invitationId
    ) {
        LobbyInvitation invitation = lobbyInvitationService.declineInvitation(token, invitationId);
        return DTOMapper.INSTANCE.convertEntityToLobbyInvitationGetDTO(invitation);
    }
}
