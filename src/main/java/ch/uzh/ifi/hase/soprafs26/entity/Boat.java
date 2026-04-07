package ch.uzh.ifi.hase.soprafs26.entity;

public class Boat {
    private String boatType;
    private int[] position;
    private int playerId;

    public String getBoatType() {
        return boatType;
    }
    
    public void setBoatType(String boatType) {
        this.boatType = boatType;
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
