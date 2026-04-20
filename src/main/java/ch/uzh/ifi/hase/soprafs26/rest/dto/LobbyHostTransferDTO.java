package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbyHostTransferDTO {
    private Long newHostParticipantId;
    private Long userId;

    public Long getNewHostParticipantId() {
        return newHostParticipantId;
    }

    public void setNewHostParticipantId(Long newHostParticipantId) {
        this.newHostParticipantId = newHostParticipantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
