package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;

public abstract class Building implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ownerPlayerId;
    private Integer intersectionId;

    public Long getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void setOwnerPlayerId(Long ownerPlayerId) {
        this.ownerPlayerId = ownerPlayerId;
    }

    public Integer getIntersectionId() {
        return intersectionId;
    }

    public void setIntersectionId(Integer intersectionId) {
        this.intersectionId = intersectionId;
    }
}