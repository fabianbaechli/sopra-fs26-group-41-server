package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UsersGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.PasswordService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UsersController {
    private final UserService userService;
    UsersController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/users/me")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UsersGetDTO getOwnProfile(@RequestHeader(value = "Authorization", required = true)  String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }
        User user = userService.getUserByToken(token);
        return DTOMapper.INSTANCE.convertEntityToUsersGetDTO(user);
    }
}
