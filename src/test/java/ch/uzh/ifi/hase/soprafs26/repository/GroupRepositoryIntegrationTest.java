package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class GroupRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GroupRepository groupRepository;

    @Test
    public void findGroupByJoinToken_existingToken_returnsGroup() {
        User owner = new User();
        owner.setUsername("alice");
        owner.setPassword("hashedPassword");
        owner.setToken("token-alice");
        owner.setStatus(UserStatus.ONLINE);
        entityManager.persistAndFlush(owner);

        Group group = new Group();
        group.setGroupName("Movie Night");
        group.setJoinToken("join-123");
        group.setOwner(owner);
        group.addMember(owner);

        entityManager.persistAndFlush(group);

        Group found = groupRepository.findGroupByJoinToken("join-123");

        assertNotNull(found);
        assertEquals(group.getId(), found.getId());
        assertEquals("Movie Night", found.getGroupName());
        assertEquals("join-123", found.getJoinToken());
        assertNotNull(found.getOwner());
        assertEquals(owner.getId(), found.getOwner().getId());
    }

    @Test
    public void findGroupByJoinToken_unknownToken_returnsNull() {
        Group found = groupRepository.findGroupByJoinToken("does-not-exist");

        assertNull(found);
    }

    @Test
    public void findGroupByGroupName_existingName_returnsGroup() {
        User owner = new User();
        owner.setUsername("bob");
        owner.setPassword("hashedPassword2");
        owner.setToken("token-bob");
        owner.setStatus(UserStatus.OFFLINE);
        entityManager.persistAndFlush(owner);

        Group group = new Group();
        group.setGroupName("Weekend Picks");
        group.setJoinToken("join-456");
        group.setOwner(owner);
        group.addMember(owner);

        entityManager.persistAndFlush(group);

        Group found = groupRepository.findGroupByGroupName("Weekend Picks");

        assertNotNull(found);
        assertEquals(group.getId(), found.getId());
        assertEquals("Weekend Picks", found.getGroupName());
        assertEquals("join-456", found.getJoinToken());
    }

    @Test
    public void findGroupByGroupName_unknownName_returnsNull() {
        Group found = groupRepository.findGroupByGroupName("does-not-exist");

        assertNull(found);
    }

    @Test
    public void findByMembersContaining_existingMember_returnsGroups() {
        User alice = new User();
        alice.setUsername("alice");
        alice.setPassword("hashedPassword");
        alice.setToken("token-alice");
        alice.setStatus(UserStatus.ONLINE);
        entityManager.persistAndFlush(alice);

        User bob = new User();
        bob.setUsername("bob");
        bob.setPassword("hashedPassword2");
        bob.setToken("token-bob");
        bob.setStatus(UserStatus.OFFLINE);
        entityManager.persistAndFlush(bob);

        Group group1 = new Group();
        group1.setGroupName("Movie Night");
        group1.setJoinToken("join-1");
        group1.setOwner(alice);
        group1.addMember(alice);
        group1.addMember(bob);
        entityManager.persistAndFlush(group1);

        Group group2 = new Group();
        group2.setGroupName("Drama Club");
        group2.setJoinToken("join-2");
        group2.setOwner(bob);
        group2.addMember(bob);
        entityManager.persistAndFlush(group2);

        List<Group> foundGroups = groupRepository.findByMembersContaining(bob);

        assertNotNull(foundGroups);
        assertEquals(2, foundGroups.size());
        assertTrue(foundGroups.stream().anyMatch(group -> group.getGroupName().equals("Movie Night")));
        assertTrue(foundGroups.stream().anyMatch(group -> group.getGroupName().equals("Drama Club")));
    }

    @Test
    public void findByMembersContaining_nonMember_returnsEmptyList() {
        User charlie = new User();
        charlie.setUsername("charlie");
        charlie.setPassword("hashedPassword3");
        charlie.setToken("token-charlie");
        charlie.setStatus(UserStatus.OFFLINE);
        entityManager.persistAndFlush(charlie);

        List<Group> foundGroups = groupRepository.findByMembersContaining(charlie);

        assertNotNull(foundGroups);
        assertTrue(foundGroups.isEmpty());
    }
}