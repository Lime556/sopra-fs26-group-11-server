package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStartGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyParticipant;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyParticipantGetDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(target = "id", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
	@Mapping(target = "winRate", ignore = true)
	@Mapping(target = "passwordHash", source = "password")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	UserGetDTO convertEntityToUserGetDTO(User user);

	UserAuthDTO convertEntityToUserAuthDTO(User user);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "capacity", target = "capacity")
	@Mapping(source = "currentParticipants", target = "currentParticipants")
	@Mapping(source = "participants", target = "participants")
	@Mapping(source = "gameId", target = "gameId")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "hostParticipant.id", target = "hostParticipantId")
	@Mapping(target = "privateLobby", expression = "java(lobby.getPassword() != null && !lobby.getPassword().isBlank())")
	LobbyGetDTO convertEntityToLobbyGetDTO(Lobby lobby);


	@Mapping(source = "id", target = "id")
	@Mapping(source = "bot", target = "bot")
	@Mapping(target = "userId", expression = "java(participant.getUser() != null ? participant.getUser().getId() : null)")
	@Mapping(target = "username", expression = "java(participant.getUser() != null ? participant.getUser().getUsername() : \"Bot\")")
	LobbyParticipantGetDTO convertEntityToLobbyParticipantGetDTO(LobbyParticipant participant);

	@Mapping(source = "id", target = "gameId")
	GameStartGetDTO convertEntityToGameStartGetDTO(Game game);
}
