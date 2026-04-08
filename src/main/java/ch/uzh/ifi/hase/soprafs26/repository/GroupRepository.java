package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs26.entity.Group; // Ensure Group is imported

import java.util.List;

@Repository("groupRepository")
public interface GroupRepository extends JpaRepository<Group, Long> {

    Group findGroupByJoinToken(String joinToken);
    Group findGroupByGroupName(String groupName);
    List<Group> findByMembersContaining(User user);
}