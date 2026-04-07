package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class LobbyGetDTO {

	private Long id;
	private String name;
	private int capacity;
	private int currentPlayers;
	private List<Long> playerIds;
	private boolean privateLobby;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getCurrentPlayers() {
		return currentPlayers;
	}

	public void setCurrentPlayers(int currentPlayers) {
		this.currentPlayers = currentPlayers;
	}

	public List<Long> getPlayerIds() {
		return playerIds;
	}

	public void setPlayerIds(List<Long> playerIds) {
		this.playerIds = playerIds;
	}

	public boolean isPrivateLobby() {
		return privateLobby;
	}

	public void setPrivateLobby(boolean privateLobby) {
		this.privateLobby = privateLobby;
	}
}
