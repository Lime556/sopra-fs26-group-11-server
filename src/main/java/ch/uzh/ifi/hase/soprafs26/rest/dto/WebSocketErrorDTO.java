package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class WebSocketErrorDTO {

    private int status;
    private String message;
    private String destination;

    public WebSocketErrorDTO() {
    }

    public WebSocketErrorDTO(int status, String message, String destination) {
        this.status = status;
        this.message = message;
        this.destination = destination;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}