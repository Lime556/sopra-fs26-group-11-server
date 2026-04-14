package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinDTO;
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
    public LobbyGetDTO createLobby(@RequestBody(required = false) LobbyPostDTO lobbyPostDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Lobby createdLobby = lobbyService.createLobby(
                extractToken(authorizationHeader),
            lobbyPostDTO == null ? null : lobbyPostDTO.getName(),
                lobbyPostDTO == null ? null : lobbyPostDTO.getCapacity(),
                lobbyPostDTO == null ? null : lobbyPostDTO.getPassword());
        LobbyGetDTO createdDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
        messaging.convertAndSend("/topic/lobbies", createdDTO);
        return createdDTO;
    }

    @GetMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO getLobbyById(
            @PathVariable Long lobbyId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        String token = extractToken(authorizationHeader);
        Lobby lobby = lobbyService.getLobbyById(lobbyId, token);
        return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
    }
    
    @PostMapping("/lobbies/{lobbyId}/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO joinLobby(@PathVariable Long lobbyId, @RequestBody LobbyJoinDTO lobbyJoinDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (lobbyJoinDTO != null && lobbyJoinDTO.getLobbyId() != null && !lobbyId.equals(lobbyJoinDTO.getLobbyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Path lobby id does not match body lobby id.");
        }

        String token = extractToken(authorizationHeader);
        Lobby updatedLobby = lobbyService.joinLobby(lobbyId, token, lobbyJoinDTO == null ? null : lobbyJoinDTO.getPassword());
        LobbyGetDTO updatedDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
        messaging.convertAndSend("/topic/lobbies", updatedDTO);
        return updatedDTO;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length()).trim();
        }
        return authorizationHeader.trim();
    }
}
