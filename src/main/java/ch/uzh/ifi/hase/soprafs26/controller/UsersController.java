package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UsersGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.MovieSearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UsersController {
    private final UserService userService;
    private final MovieSearchService movieSearchService;

    // Inject MovieSearchService into the constructor
    UsersController(UserService userService, MovieSearchService movieSearchService) {
        this.userService = userService;
        this.movieSearchService = movieSearchService;
    }

    @GetMapping("/users/me")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UsersGetDTO getOwnProfile(@RequestHeader(value = "Authorization", required = true) String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }
        User user = userService.getUserByToken(token);
        return DTOMapper.INSTANCE.convertEntityToUsersGetDTO(user);
    }

    // NEW ENDPOINT
    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UsersGetDTO getUserProfile(@PathVariable Long userId, @RequestHeader(value = "Authorization", required = true) String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        // 1. Get both the requester and the target user
        User currentUser = userService.getUserByToken(token);
        User targetUser = userService.getUserById(userId);

        // 2. Map target user to DTO
        UsersGetDTO dto = DTOMapper.INSTANCE.convertEntityToUsersGetDTO(targetUser);

        // 3. Calculate overlap dynamically and attach it to the DTO
        Integer overlapScore = movieSearchService.getUserTasteOverlap(currentUser, targetUser);
        dto.setTasteOverlap(overlapScore);

        return dto;
    }
}
