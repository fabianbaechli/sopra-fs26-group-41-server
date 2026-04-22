package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(1L);
		testUser.setUsername("testUsername");

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
	}

	@Test
	public void createUser_validInputs_success() {
		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		User createdUser = userService.createUser(testUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
	}



	@Test
	public void createUser_duplicateInputs_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
        //Commented out find by name as it no longer exists
		//Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}
    @Test
    public void getUsers_success() {
        Mockito.when(userRepository.findAll()).thenReturn(java.util.List.of(testUser));
        java.util.List<User> users = userService.getUsers();
        assertEquals(1, users.size());
    }

    @Test
    public void updateUserToken_success() {
        User updatedUser = userService.updateUserToken(testUser, "new-token");
        assertEquals("new-token", updatedUser.getToken());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
        Mockito.verify(userRepository, Mockito.times(1)).flush();
    }

    @Test
    public void updateUserStatus_success() {
        userService.updateUserStatus(testUser, UserStatus.ONLINE);
        assertEquals(UserStatus.ONLINE, testUser.getStatus());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
        Mockito.verify(userRepository, Mockito.times(1)).flush();
    }

    @Test
    public void newToken_isNotNull() {
        String token = userService.newToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void authenticated_validTokenOnline_returnsTrue() {
        testUser.setStatus(UserStatus.ONLINE);
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        assertTrue(userService.authenticated("valid-token"));
    }

    @Test
    public void authenticated_invalidToken_returnsFalse() {
        Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);
        assertFalse(userService.authenticated("invalid-token"));
    }

    @Test
    public void getUserById_validId_success() {
        Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
        User found = userService.getUserById(1L);
        assertEquals(testUser, found);
    }

    @Test
    public void getUserById_invalidId_throwsException() {
        Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        assertThrows(ResponseStatusException.class, () -> userService.getUserById(1L));
    }

    @Test
    public void getUserByUsername_success() {
        Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
        User found = userService.getUserByUsername("testUsername");
        assertEquals(testUser, found);
    }
}
