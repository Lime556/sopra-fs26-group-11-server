package ch.uzh.ifi.hase.soprafs26.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class CustomStompSessionHandler extends StompSessionHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(CustomStompSessionHandler.class);

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        logger.info("New session established: {}", session.getSessionId());
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error("WebSocket Client Error: ", exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        logger.warn("WebSocket transport error in session {}", session.getSessionId(), exception);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        logger.error("WebSocket STOMP error frame received: {} {}", headers, payload);
    }
}
