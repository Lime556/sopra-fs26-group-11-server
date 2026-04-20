package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;

public class Edge implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer intersectionAId;
    private Integer intersectionBId;
    private Road road;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIntersectionAId() {
        return intersectionAId;
    }

    public void setIntersectionAId(Integer intersectionAId) {
        this.intersectionAId = intersectionAId;
    }

    public Integer getIntersectionBId() {
        return intersectionBId;
    }

    public void setIntersectionBId(Integer intersectionBId) {
        this.intersectionBId = intersectionBId;
    }

    public Road getRoad() {
        return road;
    }

    public void setRoad(Road road) {
        this.road = road;
    }

    public boolean isOccupied() {
        return road != null;
    }
}