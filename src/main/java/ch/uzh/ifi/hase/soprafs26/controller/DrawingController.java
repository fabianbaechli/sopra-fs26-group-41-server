package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.service.DrawingService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DrawingController {

    private final UserService userService;
    private final DrawingService drawingService;

    DrawingController(UserService userService, DrawingService drawingService) {
        this.userService = userService;
        this.drawingService = drawingService;
    }

    @GetMapping("/groups/{groupId}/drawing/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public DrawingJoinResponseDTO initializeDrawingSession(
            @PathVariable Long groupId,
            @RequestHeader(value = "Authorization", required = true) String authorization
    ) {
        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        User user = userService.getUserByToken(token);

        return drawingService.initializeDrawingSession(groupId, user);
    }
}