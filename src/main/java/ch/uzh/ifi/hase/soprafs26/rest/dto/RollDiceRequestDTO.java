package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.Map;

public class RollDiceRequestDTO {
    private Map<String, Integer> discardResources;

    public Map<String, Integer> getDiscardResources() {
        return discardResources;
    }

    public void setDiscardResources(Map<String, Integer> discardResources) {
        this.discardResources = discardResources;
    }
}