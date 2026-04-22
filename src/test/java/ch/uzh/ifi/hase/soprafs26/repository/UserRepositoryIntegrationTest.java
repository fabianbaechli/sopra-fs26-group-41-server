package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void findByUsername_existingUser_returnsUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hashedPassword");
        user.setToken("token-alice");
        user.setStatus(UserStatus.OFFLINE);

        entityManager.persistAndFlush(user);

        User found = userRepository.findByUsername("alice");

        assertNotNull(found);
        assertEquals(user.getId(), found.getId());
        assertEquals("alice", found.getUsername());
        assertEquals("hashedPassword", found.getPassword());
        assertEquals("token-alice", found.getToken());
        assertEquals(UserStatus.OFFLINE, found.getStatus());
    }

    @Test
    public void findByUsername_unknownUser_returnsNull() {
        User found = userRepository.findByUsername("does-not-exist");

        assertNull(found);
    }

    @Test
    public void findByToken_existingToken_returnsUser() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("hashedPassword2");
        user.setToken("token-bob");
        user.setStatus(UserStatus.ONLINE);

        entityManager.persistAndFlush(user);

        User found = userRepository.findByToken("token-bob");

        assertNotNull(found);
        assertEquals(user.getId(), found.getId());
        assertEquals("bob", found.getUsername());
        assertEquals("token-bob", found.getToken());
        assertEquals(UserStatus.ONLINE, found.getStatus());
    }

    @Test
    public void findByToken_unknownToken_returnsNull() {
        User found = userRepository.findByToken("does-not-exist");

        assertNull(found);
    }
}