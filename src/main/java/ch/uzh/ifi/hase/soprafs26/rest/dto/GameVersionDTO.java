package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GameVersionDTO {
    private Long gameId;
    private Long gameVersion;
    private Integer chatMessageCount;
    private Integer eventLogCount;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(Long gameVersion) {
        this.gameVersion = gameVersion;
    }

    public Integer getChatMessageCount() {
        return chatMessageCount;
    }

    public void setChatMessageCount(Integer chatMessageCount) {
        this.chatMessageCount = chatMessageCount;
    }

    public Integer getEventLogCount() {
        return eventLogCount;
    }

    public void setEventLogCount(Integer eventLogCount) {
        this.eventLogCount = eventLogCount;
    }
}
