package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class LobbyGetDTO {

	private Long id;
	private int capacity;
	private int currentParticipants;
	private List<LobbyParticipantGetDTO> participants;
	private Long gameId;
	private Long hostParticipantId;
	private String name;
	private boolean privateLobby;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getCurrentParticipants() {
		return currentParticipants;
	}
	
	public void setCurrentParticipants(int currentParticipants) {
		this.currentParticipants = currentParticipants;
	}

	public List<LobbyParticipantGetDTO> getParticipants() {
			return participants;
		}

	public void setParticipants(List<LobbyParticipantGetDTO> participants) {
		this.participants = participants;
	}

	public Long getGameId() {
		return gameId;
	}

	public void setGameId(Long gameId) {
		this.gameId = gameId;
	}

	public Long getHostParticipantId() {
		return hostParticipantId;
	}

	public void setHostParticipantId(Long hostParticipantId) {
		this.hostParticipantId = hostParticipantId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isPrivateLobby() {
		return privateLobby;
	}

	public void setPrivateLobby(boolean privateLobby) {
		this.privateLobby = privateLobby;
	}
}
