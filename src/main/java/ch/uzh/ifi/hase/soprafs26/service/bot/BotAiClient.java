package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.Optional;

public interface BotAiClient {
    Optional<String> generateDecision(String prompt);
}