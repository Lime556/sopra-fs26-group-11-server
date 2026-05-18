package ch.uzh.ifi.hase.soprafs26.service.bot;

import ch.uzh.ifi.hase.soprafs26.entity.Game;

public record BotActionExecutionResult(
    Game game,
    Long playerId,
    boolean aiRequested,
    boolean aiConsultantUsed,
    boolean fallbackUsed,
    String fallbackReason
) {
}
