package ch.uzh.ifi.hase.soprafs26.service.bot;

public class BotAction {
    private final BotActionType type;
    private final Long playerId;
    private final Integer intersectionId;
    private final Integer edgeId;
    private final Integer hexId;
    private final Long targetPlayerId;
    private final String giveResource;
    private final String receiveResource;
    private final Integer giveAmount;
    private final Integer receiveAmount;

    private BotAction(
        BotActionType type,
        Long playerId,
        Integer intersectionId,
        Integer edgeId,
        Integer hexId,
        Long targetPlayerId,
        String giveResource,
        String receiveResource,
        Integer giveAmount,
        Integer receiveAmount
    ) {
        this.type = type;
        this.playerId = playerId;
        this.intersectionId = intersectionId;
        this.edgeId = edgeId;
        this.hexId = hexId;
        this.targetPlayerId = targetPlayerId;
        this.giveResource = giveResource;
        this.receiveResource = receiveResource;
        this.giveAmount = giveAmount;
        this.receiveAmount = receiveAmount;
    }

    public static BotAction of(BotActionType type, Long playerId) {
        return new BotAction(type, playerId, null, null, null, null, null, null, null, null);
    }

    public static BotAction settlement(BotActionType type, Long playerId, Integer intersectionId) {
        return new BotAction(type, playerId, intersectionId, null, null, null, null, null, null, null);
    }

    public static BotAction road(BotActionType type, Long playerId, Integer edgeId) {
        return new BotAction(type, playerId, null, edgeId, null, null, null, null, null, null);
    }

    public static BotAction robber(Long playerId, Integer hexId) {
        return new BotAction(BotActionType.MOVE_ROBBER, playerId, null, null, hexId, null, null, null, null, null);
    }

    public static BotAction bankTrade(Long playerId, String giveResource, String receiveResource, Integer giveAmount, Integer receiveAmount) {
        return new BotAction(BotActionType.BANK_TRADE, playerId, null, null, null, null, giveResource, receiveResource, giveAmount, receiveAmount);
    }

    public static BotAction playerTrade(Long playerId, Long targetPlayerId, String giveResource, String receiveResource, Integer giveAmount, Integer receiveAmount) {
        return new BotAction(BotActionType.PLAYER_TRADE, playerId, null, null, null, targetPlayerId, giveResource, receiveResource, giveAmount, receiveAmount);
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

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public String getGiveResource() {
        return giveResource;
    }

    public String getReceiveResource() {
        return receiveResource;
    }

    public Integer getGiveAmount() {
        return giveAmount;
    }

    public Integer getReceiveAmount() {
        return receiveAmount;
    }
}
