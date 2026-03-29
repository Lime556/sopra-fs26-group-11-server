package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbyJoinDTO {

    private Long lobbyId;
    private String password;

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
