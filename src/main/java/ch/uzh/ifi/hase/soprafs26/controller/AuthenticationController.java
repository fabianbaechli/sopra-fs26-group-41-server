package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.AuthenticationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.AuthenticationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.PasswordService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthenticationController {
    private final UserService userService;
    private final PasswordService passwordService;

    AuthenticationController(UserService userService, PasswordService passwordService) {
        this.userService = userService;
        this.passwordService = passwordService;
    }

    @PostMapping("/login")
    public AuthenticationGetDTO login(@RequestBody AuthenticationPostDTO authenticationPostDTO) {
        if (authenticationPostDTO.getUsername() == null || authenticationPostDTO.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all required fields were provided");
        }
        User user = userService.getUserByUsername(authenticationPostDTO.getUsername());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password or username incorrect");
        }
        if (!passwordService.matches(authenticationPostDTO.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password or username incorrect");
        }
        String token = userService.newToken();
        user = userService.updateUserToken(user, token);
        userService.updateUserStatus(user, UserStatus.ONLINE);
        return DTOMapper.INSTANCE.convertEntityToAuthenticationGetDTO(user);
    }

    @PostMapping("/logout")
    public void logout(
            @RequestHeader("Authorization") String authorization
    ) {
        String token = getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }
        User user = userService.getUserByToken(token);
        userService.updateUserStatus(user, UserStatus.OFFLINE);
        // invalidate token by setting a new one
        userService.updateUserToken(user, userService.newToken());
    }

    public static String getAuthorizationToken(String authorizationToken) {
        if (authorizationToken == null || !authorizationToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bearer token was not provided");
        }

        String token = authorizationToken.substring(7).trim();
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bearer token was not provided");
        }

        return token;
    }
}