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

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;

@RestController
public class LobbyController {

    private final LobbyService lobbyService;

    LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
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
                authorizationHeader,
                lobbyPostDTO == null ? null : lobbyPostDTO.getCapacity(),
                lobbyPostDTO == null ? null : lobbyPostDTO.getPassword());
        return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
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

        Lobby updatedLobby = lobbyService.joinLobby(lobbyId, authorizationHeader, lobbyJoinDTO == null ? null : lobbyJoinDTO.getPassword());
        return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
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

}
