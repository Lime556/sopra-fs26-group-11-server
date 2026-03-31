package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
	@Mapping(source = "currentPlayers", target = "currentPlayers")
	@Mapping(source = "users", target = "playerIds")
	LobbyGetDTO convertEntityToLobbyGetDTO(Lobby lobby);

	default List<Long> mapUsersToPlayerIds(Set<User> users) {
		if (users == null || users.isEmpty()) {
			return Collections.emptyList();
		}
		return users.stream().map(User::getId).toList();
	}
}
