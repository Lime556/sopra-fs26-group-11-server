package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class PortGetDTO {
    private Integer id;
    private Integer hexId;
    private String type;
    private List<Integer> corners;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getHexId() {
        return hexId;
    }

    public void setHexId(Integer hexId) {
        this.hexId = hexId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Integer> getCorners() {
        return corners;
    }

    public void setCorners(List<Integer> corners) {
        this.corners = corners;
    }
}
