package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class GroupController {

    private final UserService userService;
    private final GroupService groupService;

    GroupController(UserService userService, GroupService groupService) {
        this.userService = userService;
        this.groupService = groupService;
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GroupCreateResponseDTO createGroup(
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestBody GroupCreatePostDTO groupCreatePostDTO
    ) {
        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }
        User user = userService.getUserByToken(token);
        String groupName = groupCreatePostDTO.getName();
        Group group = groupService.createNewGroup(user, groupName);
        GroupCreateResponseDTO response = DTOMapper.INSTANCE.convertEntityToGroupCreateResponseDTO(group);
        response.setJoinUrl("/groups/join/" + group.getJoinToken());
        return response;
    }

    @GetMapping("/groups")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GroupsGetResponseDTO getUserGroups(
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        User user = userService.getUserByToken(token);
        List<Group> groups = groupService.getGroupsForUser(user);

        List<GroupSummaryDTO> groupSummaryDTOs = groups.stream()
                .map(DTOMapper.INSTANCE::convertEntityToGroupSummaryDTO)
                .toList();

        GroupsGetResponseDTO response = new GroupsGetResponseDTO();
        response.setGroups(groupSummaryDTOs);

        return response;
    }

    @PostMapping("/groups/join/{joinToken}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GroupJoinResponseDTO joinGroup(
            @PathVariable String joinToken,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }
        User user = userService.getUserByToken(token);

        Group joinedGroup = groupService.joinGroupByToken(joinToken, user);

        GroupJoinResponseDTO response = new GroupJoinResponseDTO();
        response.setGroupUrl(joinedGroup.getJoinToken());

        return response;
    }
}
