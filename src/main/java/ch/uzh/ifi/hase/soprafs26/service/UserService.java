package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
		newUser.setToken(newToken());
		newUser.setStatus(UserStatus.OFFLINE);
        newUser.setHasLetterboxdData(false);
        if (usernameTaken(newUser)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The provided username is not unique");
        }

		newUser = userRepository.save(newUser);
		userRepository.flush();

		return newUser;
	}

    public User updateUserToken(User user, String tokenNew) {
        user.setToken(tokenNew);
        userRepository.save(user);
        userRepository.flush();
        return user;
    }

    public void updateUserStatus(User user, UserStatus newStatus) {
        user.setStatus(newStatus);
        userRepository.save(user);
        userRepository.flush();
    }

    public String newToken() {
        return UUID.randomUUID().toString();
    }

    public boolean authenticated(String token) {
        User user = userRepository.findByToken(token);
        return user != null && user.getStatus() == UserStatus.ONLINE;
    }

    public User getUserByToken (String token) {
        return userRepository.findByToken(token);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private boolean usernameTaken(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        return userByUsername != null;
    }
}
