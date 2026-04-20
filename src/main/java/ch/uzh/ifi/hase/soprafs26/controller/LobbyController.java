package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyHostTransferDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyKickDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;


@RestController
public class LobbyController {

    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messaging;

    LobbyController(LobbyService lobbyService, SimpMessagingTemplate messaging) {
        this.lobbyService = lobbyService;
        this.messaging = messaging;
    }

    @GetMapping("/lobbies")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LobbyGetDTO> getLobbies() {
        return lobbyService.getLobbies().stream().map(DTOMapper.INSTANCE::convertEntityToLobbyGetDTO).toList();
    }
    
    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyGetDTO createLobby(
        @RequestBody(required = false) LobbyPostDTO lobbyPostDTO,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
      
        Lobby createdLobby = lobbyService.createLobby(
                authorizationHeader,
                lobbyPostDTO == null ? null : lobbyPostDTO.getCapacity(),
                lobbyPostDTO == null ? null : lobbyPostDTO.getPassword(),
                lobbyPostDTO == null ? null : lobbyPostDTO.getName());

        LobbyGetDTO createdDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
        messaging.convertAndSend("/topic/lobbies", createdDTO);
        return createdDTO;
    }
    
    @PostMapping("/lobbies/{lobbyId}/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
        
        LobbyGetDTO updatedDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
        messaging.convertAndSend("/topic/lobbies", updatedDTO);
        return updatedDTO;
    }

    @PostMapping("/lobbies/{lobbyId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
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
    @ResponseBody
    public LobbyGetDTO getLobbyById(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(
            lobbyService.getLobbyById(lobbyId, authorizationHeader)
        );
    }

    @PostMapping("/lobbies/{lobbyId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveLobby(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        lobbyService.leaveLobby(lobbyId, authorizationHeader);
        messaging.convertAndSend("/topic/lobbies", lobbyId);
    }

    @PostMapping("/lobbies/{lobbyId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeLobby(
        @PathVariable Long lobbyId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        lobbyService.closeLobby(lobbyId, authorizationHeader);
        messaging.convertAndSend("/topic/lobbies", lobbyId);
    }

    @PostMapping("/lobbies/{lobbyId}/participants/{participantId}/kick")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO kickParticipant(
        @PathVariable Long lobbyId,
        @PathVariable Long participantId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Lobby updatedLobby = lobbyService.kickParticipant(lobbyId, authorizationHeader, participantId);
        LobbyGetDTO updatedDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
        messaging.convertAndSend("/topic/lobbies", updatedDTO);
        return updatedDTO;
    }

    @PostMapping("/lobbies/{lobbyId}/host/transfer")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
        LobbyGetDTO updatedDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
        messaging.convertAndSend("/topic/lobbies", updatedDTO);
        return updatedDTO;
    }

    @PostMapping("/lobbies/{lobbyId}/kick")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
        LobbyGetDTO updatedDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
        messaging.convertAndSend("/topic/lobbies", updatedDTO);
        return updatedDTO;
    }

    @PostMapping("/lobbies/{lobbyId}/host-transfer")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
        LobbyGetDTO updatedDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
        messaging.convertAndSend("/topic/lobbies", updatedDTO);
        return updatedDTO;
    }
    

}
