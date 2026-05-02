package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;

import ch.uzh.ifi.hase.soprafs26.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FriendRequestGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FriendRequestPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.FriendRequestService;
import org.springframework.web.bind.annotation.*;


@RestController
public class FriendRequestController {
    
    private final FriendRequestService friendRequestService;

    public FriendRequestController(FriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    @PostMapping("/friend-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public FriendRequestGetDTO sendFriendRequest(
        @RequestHeader(value = "Authorization", required = false) String token,
        @RequestBody FriendRequestPostDTO friendRequestPostDTO
    ) {
        FriendRequest friendRequest = friendRequestService.sendFriendRequest(
            token, 
            friendRequestPostDTO.getReceiverId()
        );
        
        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
    }

    @PutMapping("/friend-requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public FriendRequestGetDTO acceptFriendRequest(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long requestId
    ) {
        FriendRequest friendRequest = friendRequestService.acceptFriendRequest(token, requestId);

        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
    }

    @PutMapping("/friend-requests/{requestId}/decline")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public FriendRequestGetDTO declineFriendRequest(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long requestId
    ) {
        FriendRequest friendRequest = friendRequestService.declineFriendRequest(token, requestId);

        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
    }
    
}
