package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class BoatGetDTO {
    private Integer id;
    private String boatType;
    private Integer hexId;
    private Integer firstCorner;
    private Integer secondCorner;

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
}
