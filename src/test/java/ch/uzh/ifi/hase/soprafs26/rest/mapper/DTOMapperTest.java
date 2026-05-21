package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import java.util.HashSet;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyInvitationStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyInvitation;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FriendGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FriendRequestGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyInvitationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyParticipantGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {

	// User creation mapping tests
	@Test
	public void testCreateUser_fromUserPostDTO_toUser_success() {
		// create UserPostDTO
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("username");
		userPostDTO.setPassword("testPassword");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getUsername(), user.getUsername());
		assertEquals(userPostDTO.getPassword(), user.getPasswordHash());
	}



	// User response mapping tests
	@Test
	public void testGetUser_fromUser_toUserGetDTO_success() {
		// create User
		User user = new User();
		user.setId(1L);
		user.setUsername("firstname@lastname");
		user.setUserStatus(UserStatus.OFFLINE);
		user.setToken("1");
		user.setWinRate(0.5);

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check content
		assertEquals(user.getId(), userGetDTO.getId());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getUserStatus(), userGetDTO.getUserStatus());
		assertEquals(user.getWinRate(), userGetDTO.getWinRate());
	}

	

	// User authentication mapping tests
	@Test
	public void testGetUserAuth_fromUser_toUserAuthDTO_success() {
		User user = new User();
		user.setId(1L);
		user.setUsername("firstname@lastname");
		user.setUserStatus(UserStatus.OFFLINE);
		user.setToken("token-123");
		user.setWinRate(0.5);

		UserAuthDTO userAuthDTO = DTOMapper.INSTANCE.convertEntityToUserAuthDTO(user);

		assertEquals(user.getId(), userAuthDTO.getId());
		assertEquals(user.getUsername(), userAuthDTO.getUsername());
		assertEquals(user.getUserStatus(), userAuthDTO.getUserStatus());
		assertEquals(user.getWinRate(), userAuthDTO.getWinRate());
		assertEquals(user.getToken(), userAuthDTO.getToken());
	}

	@Test
	public void testGetLobby_fromLobby_toLobbyGetDTO_success() {
		Lobby lobby = new Lobby();
		lobby.setId(1L);
		lobby.setName("Test Lobby");
		lobby.setCapacity(4);
		lobby.setName("Test Lobby");

		User player = new User();
		player.setId(55L);
		player.setUsername("player1");

		LobbyParticipant participant = new LobbyParticipant();
		participant.setId(10L);
		participant.setLobby(lobby);
		participant.setUser(player);
		participant.setBot(false);

		HashSet<LobbyParticipant> participants = new HashSet<>();
		participants.add(participant);
		lobby.setParticipants(participants);
		lobby.setHostParticipant(participant);

		LobbyGetDTO lobbyGetDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);

		assertEquals(lobby.getId(), lobbyGetDTO.getId());
		assertEquals(lobby.getName(), lobbyGetDTO.getName());
		assertEquals(lobby.getCapacity(), lobbyGetDTO.getCapacity());
		assertEquals(lobby.getCurrentParticipants(), lobbyGetDTO.getCurrentParticipants());
		assertEquals(1, lobbyGetDTO.getParticipants().size());
		assertEquals(false, lobbyGetDTO.isPrivateLobby());
		assertEquals("Test Lobby", lobbyGetDTO.getName());

		assertEquals(10L, lobbyGetDTO.getHostParticipantId());
		assertEquals(10L, lobbyGetDTO.getParticipants().get(0).getId());
		assertEquals(55L, lobbyGetDTO.getParticipants().get(0).getUserId());
		assertEquals("player1", lobbyGetDTO.getParticipants().get(0).getUsername());
		assertEquals(false, lobbyGetDTO.getParticipants().get(0).isBot());
	}

	@Test
	public void nullInputs_returnNullDTOs() {
		assertNull(DTOMapper.INSTANCE.convertUserPostDTOtoEntity(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToUserGetDTO(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToUserAuthDTO(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToLobbyParticipantGetDTO(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToGameStartGetDTO(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToLobbyInvitationGetDTO(null));
		assertNull(DTOMapper.INSTANCE.convertEntityToFriendGetDTO(null));
	}

	@Test
	public void lobbyMapping_privateLobbyAndBotParticipant_coverNestedBranches() {
		Lobby lobby = new Lobby();
		lobby.setId(2L);
		lobby.setName("Private Lobby");
		lobby.setCapacity(4);
		lobby.setPassword(" secret ");
		lobby.setHostParticipant(null);

		LobbyParticipant botParticipant = new LobbyParticipant();
		botParticipant.setId(20L);
		botParticipant.setBot(true);
		botParticipant.setUser(null);

		HashSet<LobbyParticipant> participants = new HashSet<>();
		participants.add(botParticipant);
		lobby.setParticipants(participants);

		LobbyGetDTO dto = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);

		assertEquals(true, dto.isPrivateLobby());
		assertNull(dto.getHostParticipantId());
		assertEquals(1, dto.getParticipants().size());
		LobbyParticipantGetDTO participantDTO = dto.getParticipants().get(0);
		assertNull(participantDTO.getUserId());
		assertEquals("Bot", participantDTO.getUsername());
		assertEquals(true, participantDTO.isBot());
	}

	@Test
	public void gameAndFriendMappings_coverNestedNullAndPresentBranches() {
		Game game = new Game();
		game.setId(33L);
		GameStartGetDTO gameStartGetDTO = DTOMapper.INSTANCE.convertEntityToGameStartGetDTO(game);
		assertEquals(33L, gameStartGetDTO.getGameId());

		User sender = new User();
		sender.setId(1L);
		sender.setUsername("sender");
		User receiver = new User();
		receiver.setId(2L);
		receiver.setUsername("receiver");

		FriendRequest friendRequest = new FriendRequest();
		friendRequest.setId(44L);
		friendRequest.setSender(sender);
		friendRequest.setReceiver(receiver);
		friendRequest.setStatus(FriendRequestStatus.PENDING);

		FriendRequestGetDTO requestDTO = DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
		assertEquals(44L, requestDTO.getId());
		assertEquals(1L, requestDTO.getSenderId());
		assertEquals("sender", requestDTO.getSenderUsername());
		assertEquals(2L, requestDTO.getReceiverId());
		assertEquals("receiver", requestDTO.getReceiverUsername());

		friendRequest.setSender(null);
		friendRequest.setReceiver(null);
		FriendRequestGetDTO nullNestedDTO = DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
		assertNull(nullNestedDTO.getSenderId());
		assertNull(nullNestedDTO.getSenderUsername());
		assertNull(nullNestedDTO.getReceiverId());
		assertNull(nullNestedDTO.getReceiverUsername());

		FriendGetDTO friendDTO = DTOMapper.INSTANCE.convertEntityToFriendGetDTO(sender);
		assertEquals(sender.getId(), friendDTO.getId());
		assertEquals(sender.getUsername(), friendDTO.getUsername());
	}

	@Test
	public void mapperNestedEdgeCases_coverRemainingNullBranches() {
		Lobby lobby = new Lobby();
		lobby.setPassword("   ");
		lobby.setParticipants(null);
		LobbyParticipant hostWithoutId = new LobbyParticipant();
		hostWithoutId.setId(null);
		lobby.setHostParticipant(hostWithoutId);

		LobbyGetDTO lobbyDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
		assertEquals(false, lobbyDTO.isPrivateLobby());
		assertNull(lobbyDTO.getHostParticipantId());
		assertNull(lobbyDTO.getParticipants());

		LobbyParticipant userParticipant = new LobbyParticipant();
		User user = new User();
		user.setId(77L);
		user.setUsername("seen-user");
		userParticipant.setUser(user);
		userParticipant.setLastSeenAt(Instant.parse("2026-05-20T09:00:00Z"));

		LobbyParticipantGetDTO participantDTO = DTOMapper.INSTANCE.convertEntityToLobbyParticipantGetDTO(userParticipant);
		assertEquals(77L, participantDTO.getUserId());
		assertEquals("seen-user", participantDTO.getUsername());
		assertEquals("2026-05-20T09:00:00Z", participantDTO.getLastSeenAt());

		FriendRequest request = new FriendRequest();
		request.setSender(new User());
		request.setReceiver(new User());

		FriendRequestGetDTO requestDTO = DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(request);
		assertNull(requestDTO.getSenderId());
		assertNull(requestDTO.getSenderUsername());
		assertNull(requestDTO.getReceiverId());
		assertNull(requestDTO.getReceiverUsername());
	}

	@Test
	public void lobbyInvitationMapping_mapsNestedFields() {
		User sender = new User();
		sender.setId(11L);
		sender.setUsername("sender-user");

		User receiver = new User();
		receiver.setId(22L);
		receiver.setUsername("receiver-user");

		Lobby lobby = new Lobby();
		lobby.setId(33L);
		lobby.setName("Cool Lobby");

		LobbyInvitation invitation = new LobbyInvitation();
		invitation.setId(44L);
		invitation.setLobby(lobby);
		invitation.setSender(sender);
		invitation.setReceiver(receiver);
		invitation.setStatus(LobbyInvitationStatus.PENDING);
		invitation.setCreatedAt(Instant.parse("2026-05-21T10:00:00Z"));

		LobbyInvitationGetDTO dto = DTOMapper.INSTANCE.convertEntityToLobbyInvitationGetDTO(invitation);

		assertEquals(44L, dto.getId());
		assertEquals(33L, dto.getLobbyId());
		assertEquals("Cool Lobby", dto.getLobbyName());
		assertEquals(11L, dto.getSenderId());
		assertEquals("sender-user", dto.getSenderUsername());
		assertEquals(22L, dto.getReceiverId());
		assertEquals("receiver-user", dto.getReceiverUsername());
		assertEquals(LobbyInvitationStatus.PENDING, dto.getStatus());
		assertEquals(Instant.parse("2026-05-21T10:00:00Z"), dto.getCreatedAt());
	}
}
