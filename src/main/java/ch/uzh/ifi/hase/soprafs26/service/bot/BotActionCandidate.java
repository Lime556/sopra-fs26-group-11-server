package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.Map;

public record BotActionCandidate(String id, BotAction action, Map<String, Object> promptDetails) {
    public BotActionCandidate {
        promptDetails = promptDetails == null ? Map.of() : Map.copyOf(promptDetails);
    }
}