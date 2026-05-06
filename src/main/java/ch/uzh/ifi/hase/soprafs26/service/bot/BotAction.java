package ch.uzh.ifi.hase.soprafs26.service.bot;

public class BotAction {
    private final BotActionType type;
    private final Long playerId;
    private final Integer intersectionId;
    private final Integer edgeId;
    private final Integer hexId;

    private BotAction(BotActionType type, Long playerId, Integer intersectionId, Integer edgeId, Integer hexId) {
        this.type = type;
        this.playerId = playerId;
        this.intersectionId = intersectionId;
        this.edgeId = edgeId;
        this.hexId = hexId;
    }

    public static BotAction of(BotActionType type, Long playerId) {
        return new BotAction(type, playerId, null, null, null);
    }

    public static BotAction settlement(BotActionType type, Long playerId, Integer intersectionId) {
        return new BotAction(type, playerId, intersectionId, null, null);
    }

    public static BotAction road(BotActionType type, Long playerId, Integer edgeId) {
        return new BotAction(type, playerId, null, edgeId, null);
    }

    public static BotAction robber(Long playerId, Integer hexId) {
        return new BotAction(BotActionType.MOVE_ROBBER, playerId, null, null, hexId);
    }

    public BotActionType getType() {
        return type;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Integer getIntersectionId() {
        return intersectionId;
    }

    public Integer getEdgeId() {
        return edgeId;
    }

    public Integer getHexId() {
        return hexId;
    }
}
