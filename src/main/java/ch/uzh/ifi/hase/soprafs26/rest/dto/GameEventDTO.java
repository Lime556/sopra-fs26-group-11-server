package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GameEventDTO {

    private String type;
    private Long sourcePlayerId;
    private Long targetPlayerId;
    private String giveResource;
    private String receiveResource;
    private Integer amount;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
