package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final UserService userService;

    public AuthChannelInterceptor(@Lazy UserService userService) {
        this.userService = userService;
    }

    @SuppressWarnings("all")
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (message == null) {
            return null;
        }

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            String token = (authorization != null && !authorization.isEmpty()) 
                ? authorization.get(0) 
                : accessor.getFirstNativeHeader("token");

            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Unauthorized: missing Authorization header");
            }

            try {
                User user = userService.authenticate(token);
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    sessionAttributes.put("authToken", token);
                    sessionAttributes.put("authUserId", user.getId());
                    sessionAttributes.put("authUsername", user.getUsername());
                }
                accessor.setUser(user::getUsername);
            } catch (Exception e) {
                // Throwing an exception here stops the connection attempt
                throw new IllegalArgumentException("Unauthorized: " + e.getMessage(), e);
            }
        }
        return message;
    }
}