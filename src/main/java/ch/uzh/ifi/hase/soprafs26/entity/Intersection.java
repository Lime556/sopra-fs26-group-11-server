package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;

public class Intersection implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Building building;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public boolean isOccupied() {
        return building != null;
    }
}