package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
