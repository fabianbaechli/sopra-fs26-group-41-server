package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.AppWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebAppConfiguration
@SpringBootTest
@Transactional
class DrawingServiceIntegrationTest {

    @Autowired
    private DrawingService drawingService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private AppWebSocketHandler appWebSocketHandler;

    @MockitoBean
    private MovieSearchService movieSearchService;

    private User owner;
    private User member;
    private Group group;

    @BeforeEach
    void setup() {
        groupRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser("owner", "owner-token");
        member = createUser("member", "member-token");

        group = new Group();
        group.setGroupName("Movie Club");
        group.setJoinToken("join-token");
        group.setOwner(owner);
        group.addMember(owner);
        group.addMember(member);
        group = groupRepository.saveAndFlush(group);
    }

    @Test
    void initializeDrawingSession_existingGroupMember_success() {
        DrawingJoinResponseDTO response = drawingService.initializeDrawingSession(group.getId(), owner);

        assertNotNull(response.getSessionId());
        assertEquals(owner.getId(), response.getUserId());
        assertNotNull(response.getDrawingState());
        assertTrue(response.getDrawingState().getStrokes().isEmpty());

        verify(appWebSocketHandler, times(2)).sendToUser(eq("owner"), anyString());
    }

    @Test
    void initializeDrawingSession_existingGroupButUserIsNotMember_throwsForbidden() {
        User outsider = createUser("outsider", "outsider-token");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingService.initializeDrawingSession(group.getId(), outsider)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(appWebSocketHandler, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void initializeDrawingSession_missingGroup_throwsNotFound() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingService.initializeDrawingSession(999999L, owner)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(appWebSocketHandler, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void saveDrawingAndCloseSession_validMember_persistsProfilePictureAndClosesSession() {
        DrawingJoinResponseDTO joinResponse = drawingService.initializeDrawingSession(group.getId(), owner);
        reset(appWebSocketHandler);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "drawing.png",
                "image/png",
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );

        String imageResponse = drawingService.saveDrawingAndCloseSession(
                group.getId(),
                joinResponse.getSessionId(),
                image,
                owner
        );

        assertEquals("data:image/png;base64,ZmFrZS1pbWFnZQ==", imageResponse);

        assertArrayEquals(
                "fake-image".getBytes(StandardCharsets.UTF_8),
                groupRepository.findById(group.getId()).orElseThrow().getProfilePicture()
        );

        verify(appWebSocketHandler).sendToUser(eq("owner"), contains("\"event\":\"closed\""));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingService.saveDrawingAndCloseSession(group.getId(), joinResponse.getSessionId(), image, owner)
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    private User createUser(String username, String token) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setToken(token);
        user.setStatus(UserStatus.ONLINE);
        return userRepository.saveAndFlush(user);
    }
}