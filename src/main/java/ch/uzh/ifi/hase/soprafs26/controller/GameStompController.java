package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBuildActionDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RollDiceRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WebSocketErrorDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

@Controller
public class GameStompController {

    private final GameController gameController;
    private final GameService gameService;
    private final UserService userService;

    public GameStompController(GameController gameController, GameService gameService, UserService userService) {
        this.gameController = gameController;
        this.gameService = gameService;
        this.userService = userService;
    }

    @MessageMapping("/games/{gameId}/events")
    public void publishGameEvent(@DestinationVariable Long gameId,
            @Payload GameEventDTO gameEventDTO,
            SimpMessageHeaderAccessor headers) {
        String token = requireToken(headers);
        enforcePlayerOwnership(gameId, token, gameEventDTO == null ? null : gameEventDTO.getSourcePlayerId());
        gameController.publishGameEvent(gameId, gameEventDTO, token);
    }

    @MessageMapping("/games/{gameId}/chat")
    public void publishGameChatMessage(@DestinationVariable Long gameId,
            @Payload GameChatMessageDTO gameChatMessageDTO,
            SimpMessageHeaderAccessor headers) {
        String token = requireToken(headers);
        User authenticatedUser = userService.authenticate(token);
        GameChatMessageDTO payload = gameChatMessageDTO == null ? new GameChatMessageDTO() : gameChatMessageDTO;
        payload.setPlayerId(authenticatedUser.getId());
        if (payload.getPlayerName() == null || payload.getPlayerName().isBlank()) {
            payload.setPlayerName(authenticatedUser.getUsername());
        }
        gameController.publishGameChatMessage(gameId, payload, token);
    }

    @MessageMapping("/games/{gameId}/actions/roll-dice")
    public GameStateDTO rollDice(@DestinationVariable Long gameId,
            @Payload(required = false) RollDiceRequestDTO request,
            SimpMessageHeaderAccessor headers) {
        return gameController.rollDice(gameId, requireToken(headers), request);
    }

    @MessageMapping("/games/{gameId}/actions/move-robber")
    public GameStateDTO moveRobber(@DestinationVariable Long gameId,
            @Payload Integer hexId,
            SimpMessageHeaderAccessor headers) {
        return gameController.moveRobber(gameId, hexId, requireToken(headers));
    }

    @MessageMapping("/games/{gameId}/actions/end-turn")
    public GameStateDTO endTurn(@DestinationVariable Long gameId,
            SimpMessageHeaderAccessor headers) {
        return gameController.endTurn(gameId, requireToken(headers));
    }

    @MessageMapping("/games/{gameId}/actions/build-settlement")
    public void buildSettlement(@DestinationVariable Long gameId,
            @Payload GameBuildActionDTO actionDTO,
            SimpMessageHeaderAccessor headers) {
        String token = requireToken(headers);
        enforcePlayerOwnership(gameId, token, actionDTO == null ? null : actionDTO.getPlayerId());
        Map<String, Object> body = new HashMap<>();
        body.put("playerId", actionDTO == null ? null : actionDTO.getPlayerId());
        body.put("intersectionId", actionDTO == null ? null : actionDTO.getIntersectionId());
        gameController.buildSettlement(gameId, body, token);
    }

    @MessageMapping("/games/{gameId}/actions/build-road")
    public void buildRoad(@DestinationVariable Long gameId,
            @Payload GameBuildActionDTO actionDTO,
            SimpMessageHeaderAccessor headers) {
        String token = requireToken(headers);
        enforcePlayerOwnership(gameId, token, actionDTO == null ? null : actionDTO.getPlayerId());
        Map<String, Object> body = new HashMap<>();
        body.put("playerId", actionDTO == null ? null : actionDTO.getPlayerId());
        body.put("edgeId", actionDTO == null ? null : actionDTO.getEdgeId());
        gameController.buildRoad(gameId, body, token);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public WebSocketErrorDTO handleException(Exception exception) {
        String message = Objects.requireNonNullElse(exception.getMessage(), "Unexpected websocket error");
        if (exception instanceof ResponseStatusException responseStatusException) {
            return new WebSocketErrorDTO(
                responseStatusException.getStatusCode().value(),
                responseStatusException.getReason() == null ? responseStatusException.getMessage() : responseStatusException.getReason(),
                responseStatusException.getStatusCode().toString()
            );
        }

        return new WebSocketErrorDTO(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message,
            HttpStatus.INTERNAL_SERVER_ERROR.toString()
        );
    }

    private String requireToken(SimpMessageHeaderAccessor headers) {
        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        Object token = sessionAttributes == null ? null : sessionAttributes.get("authToken");
        if (token instanceof String tokenValue && !tokenValue.isBlank()) {
            return tokenValue;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing websocket authentication token.");
    }

    private void enforcePlayerOwnership(Long gameId, String token, Long playerId) {
        if (playerId == null) {
            return;
        }

        User authenticatedUser = userService.authenticate(token);
        Game game = gameService.getGameById(gameId, token);
        Player player = game.getPlayers() == null
                ? null
                : game.getPlayers().stream()
                    .filter(candidate -> Objects.equals(candidate.getId(), playerId))
                    .findFirst()
                    .orElse(null);

        if (player == null || player.getUser() == null || !Objects.equals(player.getUser().getId(), authenticatedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to act on behalf of this player.");
        }
    }
}