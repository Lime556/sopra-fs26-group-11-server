package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;

public class Road implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ownerPlayerId;
    private Integer edgeId;

    public Long getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void setOwnerPlayerId(Long ownerPlayerId) {
        this.ownerPlayerId = ownerPlayerId;
    }

    public Integer getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(Integer edgeId) {
        this.edgeId = edgeId;
    }
}