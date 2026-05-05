package ch.uzh.ifi.hase.soprafs26.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs26.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.FriendService;

@WebMvcTest(FriendController.class)
public class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendService friendService;






    // ============ Send Friend Request Tests ============

    @Test
    public void sendFriendRequest_validInput_returnsCreatedRequest() throws Exception {
        User sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");

        User receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");

        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreatedAt(Instant.parse("2026-05-02T12:00:00Z"));

        given(friendService.sendFriendRequest("sender-token", 2L)).willReturn(request);

        String body = """
            {
              "receiverId": 2
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/friend-requests")
            .header("Authorization", "sender-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);

        mockMvc.perform(postRequest)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(100)))
            .andExpect(jsonPath("$.senderId", is(1)))
            .andExpect(jsonPath("$.receiverId", is(2)))
            .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    public void sendFriendRequest_missingToken_returnsUnauthorized() throws Exception {
        given(friendService.sendFriendRequest(null, 2L))
            .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated"));

        String body = """
            {
              "receiverId": 2
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/friend-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);

        mockMvc.perform(postRequest)
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void sendFriendRequest_toSelf_returnsBadRequest() throws Exception {
        given(friendService.sendFriendRequest("sender-token", 1L))
            .willThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "You cannot send a friend request to yourself."
            ));

        String body = """
            {
              "receiverId": 1
            }
            """;

        MockHttpServletRequestBuilder postRequest = post("/friend-requests")
            .header("Authorization", "sender-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);

        mockMvc.perform(postRequest)
            .andExpect(status().isBadRequest());
    }






    // ============ Accept Friend Request Tests ============

    @Test
    public void acceptFriendRequest_validInput_returnsAcceptedRequest() throws Exception {
        User sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");

        User receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");

        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setCreatedAt(Instant.parse("2026-05-02T12:00:00Z"));

        given(friendService.acceptFriendRequest("receiver-token", 100L)).willReturn(request);

        MockHttpServletRequestBuilder putRequest = put("/friend-requests/100/accept")
            .header("Authorization", "receiver-token");

        mockMvc.perform(putRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(100)))
            .andExpect(jsonPath("$.senderId", is(1)))
            .andExpect(jsonPath("$.receiverId", is(2)))
            .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }

    @Test
    public void acceptFriendRequest_notReceiver_returnsForbidden() throws Exception {
        given(friendService.acceptFriendRequest("sender-token", 100L))
            .willThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the receiver can accept this friend request."
            ));

        MockHttpServletRequestBuilder putRequest = put("/friend-requests/100/accept")
            .header("Authorization", "sender-token");

        mockMvc.perform(putRequest)
            .andExpect(status().isForbidden());
    }

    @Test
    public void acceptFriendRequest_notFound_returnsNotFound() throws Exception {
        given(friendService.acceptFriendRequest("receiver-token", 999L))
            .willThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Friend request not found."
            ));

        MockHttpServletRequestBuilder putRequest = put("/friend-requests/999/accept")
            .header("Authorization", "receiver-token");

        mockMvc.perform(putRequest)
            .andExpect(status().isNotFound());
    }






    // ============ Decline Friend Request Tests ============

    @Test
    public void declineFriendRequest_validInput_returnsDeclinedRequest() throws Exception {
        User sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");

        User receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");

        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.DECLINED);
        request.setCreatedAt(Instant.parse("2026-05-02T12:00:00Z"));

        given(friendService.declineFriendRequest("receiver-token", 100L)).willReturn(request);

        MockHttpServletRequestBuilder putRequest = put("/friend-requests/100/decline")
            .header("Authorization", "receiver-token");

        mockMvc.perform(putRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(100)))
            .andExpect(jsonPath("$.senderId", is(1)))
            .andExpect(jsonPath("$.receiverId", is(2)))
            .andExpect(jsonPath("$.status", is("DECLINED")));
    }

    @Test
    public void declineFriendRequest_notReceiver_returnsForbidden() throws Exception {
        given(friendService.declineFriendRequest("sender-token", 100L))
            .willThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the receiver can decline this friend request."
            ));

        MockHttpServletRequestBuilder putRequest = put("/friend-requests/100/decline")
            .header("Authorization", "sender-token");

        mockMvc.perform(putRequest)
            .andExpect(status().isForbidden());
    }

    @Test
    public void declineFriendRequest_notFound_returnsNotFound() throws Exception {
        given(friendService.declineFriendRequest("receiver-token", 999L))
            .willThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Friend request not found."
            ));

        MockHttpServletRequestBuilder putRequest = put("/friend-requests/999/decline")
            .header("Authorization", "receiver-token");

        mockMvc.perform(putRequest)
            .andExpect(status().isNotFound());
    }







    // ============ Get Friends Tests ============

    @Test
    public void getFriends_validToken_returnsFriends() throws Exception {
        User friend = new User();
        friend.setId(2L);
        friend.setUsername("receiver");
        friend.setEmail("receiver@email.com");

        given(friendService.getFriends("sender-token")).willReturn(List.of(friend));

        MockHttpServletRequestBuilder getRequest = get("/friends")
            .header("Authorization", "sender-token");

        mockMvc.perform(getRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(2)))
            .andExpect(jsonPath("$[0].username", is("receiver")));
    }

    @Test
    public void getFriends_noFriends_returnsEmptyList() throws Exception {
        given(friendService.getFriends("sender-token")).willReturn(List.of());

        MockHttpServletRequestBuilder getRequest = get("/friends")
            .header("Authorization", "sender-token");

        mockMvc.perform(getRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getFriends_missingToken_returnsUnauthorized() throws Exception {
        given(friendService.getFriends(null))
            .willThrow(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "not authenticated"
            ));

        MockHttpServletRequestBuilder getRequest = get("/friends");

        mockMvc.perform(getRequest)
            .andExpect(status().isUnauthorized());
    }


}