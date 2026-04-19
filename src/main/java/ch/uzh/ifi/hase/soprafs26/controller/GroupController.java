package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.FetchedMovie;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupCreatePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupCreateResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupDetailsResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupLeaveResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupRecommendationDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupSummaryDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupsGetResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecommendedMovieDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import ch.uzh.ifi.hase.soprafs26.service.PollService;

@RestController
public class GroupController {

    private final UserService userService;
    private final GroupService groupService;
    private final PollService pollService;

    GroupController(UserService userService, GroupService groupService, PollService pollService) {
        this.userService = userService;
        this.groupService = groupService;
        this.pollService = pollService;
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

    @GetMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GroupDetailsResponseDTO getGroupDetails(
            @PathVariable Long groupId,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        User currentUser = userService.getUserByToken(token);
        Group group = groupService.getGroupById(groupId);

        if (!groupService.isMember(group, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        GroupDetailsResponseDTO response = DTOMapper.INSTANCE.convertEntityToGroupDetailsResponseDTO(group);
        response.setJoinUrl("/groups/join/" + group.getJoinToken());
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
        groupService.recommendMovies(joinedGroup,0); //Offset is at zero when creating a group

        GroupJoinResponseDTO response = new GroupJoinResponseDTO();
        response.setGroupUrl(joinedGroup.getJoinToken());

        return response;
    }

    @PostMapping("/groups/{groupId}/leave")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GroupLeaveResponseDTO leaveGroup(
            @PathVariable Long groupId,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        User user = userService.getUserByToken(token);
        groupService.leaveGroup(groupId, user);

        GroupLeaveResponseDTO response = new GroupLeaveResponseDTO();
        response.setMessage("User successfully left the group");

        return response;
    }

    @GetMapping("/groups/{groupId}/recommendations")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GroupRecommendationDTO getGroupRecommendations(
            @PathVariable Long groupId,
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestHeader(value = "Offset", required = false, defaultValue = "0") int offset) { // <-- Added Header

        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        User user = userService.getUserByToken(token);
        Group group = groupService.getGroupById(groupId);

        // Security check: Ensure the caller is actually in the group
        if (!groupService.isMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        // 1. Check if recommendations already exist to save compute
        List<FetchedMovie> fetchedMovies = group.getRecommendedMovies();

        // 2. Fetch on the fly if cache is empty OR if we are paginating (offset > 0)
        if (fetchedMovies == null || fetchedMovies.isEmpty() || offset > 0) {

            // This throws the required 409 Conflict if no members have data
            groupService.checkRecommendedMoviesEligibility(group);

            // Generate the movies and save them to the group database
            fetchedMovies = groupService.recommendMovies(group, offset); // <-- Pass offset to service
        }

        // 3. Map the fetched (or newly generated) entities to DTOs
        List<RecommendedMovieDTO> movieDTOs = fetchedMovies.stream()
                .map(DTOMapper.INSTANCE::convertEntityToRecommendedMovieDTO)
                .toList();

        // 4. Build and return the final response object matching your spec
        GroupRecommendationDTO response = new GroupRecommendationDTO();
        response.setGroupId(groupId);
        response.setRecommendations(movieDTOs);

        return response;
    }

    @PostMapping("/groups/{groupId}/poll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startPoll(
            @PathVariable Long groupId,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        User user = userService.getUserByToken(token);
        pollService.startPoll(groupId, user);
    }
}
