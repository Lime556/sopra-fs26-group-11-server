package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbyPostDTO {

    private Integer capacity;
    private String password;

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
