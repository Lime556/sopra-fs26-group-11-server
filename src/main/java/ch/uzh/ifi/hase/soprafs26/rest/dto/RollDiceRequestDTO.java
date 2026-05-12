package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.Map;

public class RollDiceRequestDTO {
    private Long expectedGameVersion;
    private Map<String, Integer> discardResources;

    public Long getExpectedGameVersion() {
        return expectedGameVersion;
    }

    public void setExpectedGameVersion(Long expectedGameVersion) {
        this.expectedGameVersion = expectedGameVersion;
    }

    public Map<String, Integer> getDiscardResources() {
        return discardResources;
    }

    public void setDiscardResources(Map<String, Integer> discardResources) {
        this.discardResources = discardResources;
    }
}
