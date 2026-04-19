package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.PollBroadcastService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class PollService {

    private final GroupService groupService;
    private final PollBroadcastService pollBroadcastService;
    private final Set<Long> activePollGroupIds = ConcurrentHashMap.newKeySet();

    public PollService(GroupService groupService, PollBroadcastService pollBroadcastService) {
        this.groupService = groupService;
        this.pollBroadcastService = pollBroadcastService;
    }

    public void startPoll(Long groupId, User user) {
        Group group = groupService.getGroupById(groupId);

        if (!groupService.isMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        if (!activePollGroupIds.add(groupId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another poll is already running");
        }

        try {
            if (group.getRecommendedMovies() == null || group.getRecommendedMovies().isEmpty()) {
                groupService.checkRecommendedMoviesEligibility(group);
                groupService.recommendMovies(group,0); //Temporarily set offset to zero we can start to change this and make it more sensible
            }

            pollBroadcastService.broadcastPollStarted(group);
        } catch (RuntimeException e) {
            activePollGroupIds.remove(groupId);
            throw e;
        }
    }
}
