package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.Map;

public class GameEventDTO {

    private String type;
    private Long sourcePlayerId;
    private Long targetPlayerId;
    private String giveResource;
    private String receiveResource;
    private Integer amount;
    private Integer giveAmount;
    private Integer receiveAmount;
    private Map<String, Integer> giveResources;
    private Map<String, Integer> receiveResources;
    private String tradeAction;
    private String tradeRequestId;
    private Integer hexId;
    private Integer edge;
    private Integer intersectionId;
    private String developmentCard;
    private String secondResource;
    private String message;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSourcePlayerId() {
        return sourcePlayerId;
    }

    public void setSourcePlayerId(Long sourcePlayerId) {
        this.sourcePlayerId = sourcePlayerId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public String getGiveResource() {
        return giveResource;
    }

    public void setGiveResource(String giveResource) {
        this.giveResource = giveResource;
    }

    public String getReceiveResource() {
        return receiveResource;
    }

    public void setReceiveResource(String receiveResource) {
        this.receiveResource = receiveResource;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getGiveAmount() {
        return giveAmount;
    }

    public void setGiveAmount(Integer giveAmount) {
        this.giveAmount = giveAmount;
    }

    public Integer getReceiveAmount() {
        return receiveAmount;
    }

    public void setReceiveAmount(Integer receiveAmount) {
        this.receiveAmount = receiveAmount;
    }

    public Map<String, Integer> getGiveResources() {
        return giveResources;
    }

    public void setGiveResources(Map<String, Integer> giveResources) {
        this.giveResources = giveResources;
    }

    public Map<String, Integer> getReceiveResources() {
        return receiveResources;
    }

    public void setReceiveResources(Map<String, Integer> receiveResources) {
        this.receiveResources = receiveResources;
    }

    public String getTradeAction() {
        return tradeAction;
    }

    public void setTradeAction(String tradeAction) {
        this.tradeAction = tradeAction;
    }

    public String getTradeRequestId() {
        return tradeRequestId;
    }

    public void setTradeRequestId(String tradeRequestId) {
        this.tradeRequestId = tradeRequestId;
    }

    public Integer getHexId() {
        return hexId;
    }

    public void setHexId(Integer hexId) {
        this.hexId = hexId;
    }

    public Integer getEdge() {
        return edge;
    }

    public void setEdge(Integer edge) {
        this.edge = edge;
    }

    public Integer getIntersectionId() {
        return intersectionId;
    }

    public void setIntersectionId(Integer intersectionId) {
        this.intersectionId = intersectionId;
    }

    public String getDevelopmentCard() {
        return developmentCard;
    }

    public void setDevelopmentCard(String developmentCard) {
        this.developmentCard = developmentCard;
    }

    public String getSecondResource() {
        return secondResource;
    }

    public void setSecondResource(String secondResource) {
        this.secondResource = secondResource;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}