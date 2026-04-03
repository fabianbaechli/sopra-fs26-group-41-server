package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.service.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

@RestController
public class GroupController {

	private final UserService userService;
    private final GroupService groupService;

	RegistrationController(UserService userService, PasswordService passwordService) {
		this.userService = userService;
        this.groupService = groupService;
	}

@PostMapping("/groups/join/{joinToken}")
@ResponseStatus(HttpStatus.OK)
@ResponseBody
public GroupJoinResponseDTO joinGroup(
    @PathVariable String joinToken, 
    @RequestHeader(value = "Authorization", required = true) String token) {
    
    // 1. Identify the user via the service using the provided token
    User user = userService.findUserByToken(token); 
    
    // 2. Delegate the join logic to the service
    Group joinedGroup = groupService.joinGroupByToken(joinToken, user);
    
    // 3. Return the response DTO containing the groupUrl (inviteLink)
    GroupJoinResponseDTO response = new GroupJoinResponseDTO();
    response.setGroupUrl(joinedGroup.getInviteLink());
    
    return response;
}
@PostMapping("/groups/{groupId}/leave")
@ResponseStatus(HttpStatus.OK)
@ResponseBody
public GroupLeaveResponseDTO leaveGroup(
    @PathVariable Long groupId, 
    @RequestHeader(value = "Authorization", required = true) String token) {
        
    // 1. Authenticate user
    User user = userService.findUserByToken(token); 
        
    // 2. Delegate leave logic
    groupService.leaveGroup(groupId, user);
        
    // 3. Return success response
    GroupLeaveResponseDTO response = new GroupLeaveResponseDTO();
    response.setMessage("User successfully left the group");
        
    return response;
    }
}
