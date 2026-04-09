package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;

public class Boat implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String boatType;
    private Integer hexId;
    private Integer firstCorner;
    private Integer secondCorner;
    private int[] position;
    private int playerId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBoatType() {
        return boatType;
    }
    
    public void setBoatType(String boatType) {
        this.boatType = boatType;
    }

    public Integer getHexId() {
        return hexId;
    }

    public void setHexId(Integer hexId) {
        this.hexId = hexId;
    }

    public Integer getFirstCorner() {
        return firstCorner;
    }

    public void setFirstCorner(Integer firstCorner) {
        this.firstCorner = firstCorner;
    }

    public Integer getSecondCorner() {
        return secondCorner;
    }

    public void setSecondCorner(Integer secondCorner) {
        this.secondCorner = secondCorner;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }
}
