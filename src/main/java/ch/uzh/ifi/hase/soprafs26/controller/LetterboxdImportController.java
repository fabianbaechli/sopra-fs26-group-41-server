package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LetterboxdImportResponse;
import ch.uzh.ifi.hase.soprafs26.service.LetterboxdImportService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
public class LetterboxdImportController {
    private final UserService userService;
    private final LetterboxdImportService letterboxdImportService;

    LetterboxdImportController(UserService userService, LetterboxdImportService letterboxdImportService) {
        this.userService = userService;
        this.letterboxdImportService = letterboxdImportService;
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LetterboxdImportResponse> importLetterboxdData(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authorization
            ) throws IOException {
        String token = AuthenticationController.getAuthorizationToken(authorization);

        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }
        User currentUser = userService.getUserByToken(token);

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only zip files are allowed");
        }

        LetterboxdImportResponse response = letterboxdImportService.importZip(currentUser, file);
        return ResponseEntity.ok(response);
    }


}
