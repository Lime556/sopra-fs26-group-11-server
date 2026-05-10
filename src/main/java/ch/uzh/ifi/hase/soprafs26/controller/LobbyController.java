package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyHostTransferDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyKickDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyParticipantGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;


@RestController
public class LobbyController {

    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping("/lobbies")
    @ResponseStatus(HttpStatus.OK)
    public List<LobbyGetDTO> getLobbies() {
        return lobbyService.getLobbies().stream().map(this::convertLobbyToDto).toList();
    }
    
    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyGetDTO createLobby(
        @RequestBody(required = false) LobbyPostDTO lobbyPostDTO,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
      
        Lobby createdLobby = lobbyService.createLobby(
                authorizationHeader,
                lobbyPostDTO == null ? null : lobbyPostDTO.getCapacity(),
                lobbyPostDTO == null ? null : lobbyPostDTO.getPassword(),
                lobbyPostDTO == null ? null : lobbyPostDTO.getName());

        return convertLobbyToDto(createdLobby);
    }
    
    @PostMapping("/lobbies/{lobbyId}/join")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO joinLobby(
        @PathVariable Long lobbyId,
        @RequestBody LobbyJoinDTO lobbyJoinDTO,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (lobbyJoinDTO != null && lobbyJoinDTO.getLobbyId() != null && !lobbyId.equals(lobbyJoinDTO.getLobbyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Path lobby id does not match body lobby id.");
        }

        Lobby updatedLobby = lobbyService.joinLobby(
          lobbyId, 
          authorizationHeader, 
          lobbyJoinDTO == null ? null : lobbyJoinDTO.getPassword());
        
        return convertLobbyToDto(updatedLobby);
    }

    @PostMapping("/lobbies/{lobbyId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    public GameStartGetDTO startGame (
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return DTOMapper.INSTANCE.convertEntityToGameStartGetDTO(
            lobbyService.startGame(lobbyId, authorizationHeader)
        );
    }

    @GetMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO getLobbyById(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return convertLobbyToDto(
            lobbyService.getLobbyById(lobbyId, authorizationHeader)
        );
    }

    @PostMapping("/lobbies/{lobbyId}/heartbeat")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO heartbeatLobby(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return convertLobbyToDto(
            lobbyService.heartbeatLobby(lobbyId, authorizationHeader)
        );
    }

    @PostMapping("/lobbies/{lobbyId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveLobby(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        lobbyService.leaveLobby(lobbyId, authorizationHeader);
    }

    @PostMapping("/lobbies/{lobbyId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeLobby(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        lobbyService.closeLobby(lobbyId, authorizationHeader);
    }

    @PostMapping("/lobbies/{lobbyId}/participants/{participantId}/kick")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO kickParticipant(
        @PathVariable Long lobbyId,
        @PathVariable Long participantId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Lobby updatedLobby = lobbyService.kickParticipant(lobbyId, authorizationHeader, participantId);
        return convertLobbyToDto(updatedLobby);
    }

    @PostMapping("/lobbies/{lobbyId}/bots")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO addBot(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Lobby updatedLobby = lobbyService.addBot(lobbyId, authorizationHeader);
        return convertLobbyToDto(updatedLobby);
    }

    @PostMapping("/lobbies/{lobbyId}/bots/{participantId}/remove")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO removeBot(
        @PathVariable Long lobbyId,
        @PathVariable Long participantId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Lobby updatedLobby = lobbyService.removeBot(lobbyId, authorizationHeader, participantId);
        return convertLobbyToDto(updatedLobby);
    }

    @PostMapping("/lobbies/{lobbyId}/host/transfer")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO transferHost(
        @PathVariable Long lobbyId,
        @RequestBody LobbyHostTransferDTO transferDTO,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Lobby updatedLobby = lobbyService.transferHost(
            lobbyId,
            authorizationHeader,
            transferDTO == null ? null : transferDTO.getNewHostParticipantId()
        );
        return convertLobbyToDto(updatedLobby);
    }

    @PostMapping("/lobbies/{lobbyId}/kick")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO kickPlayer(
        @PathVariable Long lobbyId,
        @RequestBody LobbyKickDTO kickDTO,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Lobby updatedLobby = lobbyService.kickPlayer(
            lobbyId,
            authorizationHeader,
            kickDTO == null ? null : kickDTO.getUserId()
        );
        return convertLobbyToDto(updatedLobby);
    }

    @PostMapping("/lobbies/{lobbyId}/host-transfer")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO transferHostByUser(
        @PathVariable Long lobbyId,
        @RequestBody LobbyHostTransferDTO transferDTO,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Lobby updatedLobby = lobbyService.transferHostToUser(
            lobbyId,
            authorizationHeader,
            transferDTO == null ? null : transferDTO.getUserId()
        );
        return convertLobbyToDto(updatedLobby);
    }

    private LobbyGetDTO convertLobbyToDto(Lobby lobby) {
        LobbyGetDTO dto = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
        if (dto.getParticipants() == null) {
            return dto;
        }

        int botCount = 0;
        for (LobbyParticipantGetDTO participant : dto.getParticipants()) {
            if (participant != null && participant.isBot()) {
                botCount++;
                participant.setUsername("Bot " + botCount);
            }
        }
        return dto;
    }
    

}
