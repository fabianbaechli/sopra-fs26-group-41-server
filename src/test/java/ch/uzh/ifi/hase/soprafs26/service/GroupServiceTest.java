package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecommendResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MovieSearchService movieSearchService;

    @InjectMocks
    private GroupService groupService;

    private User testUser;
    private Group testGroup;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setId(1L);

        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.setGroupName("TestGroup");
        testGroup.setJoinToken("valid-token");
        testGroup.setOwner(testUser);
        testGroup.addMember(testUser);
    }

    @Test
    public void createNewGroup_validInputs_success() {
        Mockito.when(groupRepository.findGroupByGroupName("TestGroup")).thenReturn(null);
        Mockito.when(groupRepository.save(Mockito.any(Group.class))).thenAnswer(i -> i.getArgument(0));

        Group createdGroup = groupService.createNewGroup(testUser, "TestGroup");

        assertNotNull(createdGroup);
        assertEquals("TestGroup", createdGroup.getGroupName());
        assertEquals(testUser, createdGroup.getOwner());
        assertNotNull(createdGroup.getJoinToken());
        Mockito.verify(groupRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    public void createNewGroup_duplicateName_throwsException() {
        Mockito.when(groupRepository.findGroupByGroupName("TestGroup")).thenReturn(testGroup);

        assertThrows(ResponseStatusException.class, () -> groupService.createNewGroup(testUser, "TestGroup"));
    }

    @Test
    public void getGroupById_validId_success() {
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        Group foundGroup = groupService.getGroupById(1L);

        assertEquals(testGroup, foundGroup);
    }

    @Test
    public void getGroupById_invalidId_throwsException() {
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> groupService.getGroupById(1L));
    }
    @Test
    public void joinGroupByToken_validToken_success() {
        User newUser = new User();
        newUser.setUsername("newUser");

        Mockito.when(groupRepository.findGroupByJoinToken("valid-token")).thenReturn(testGroup);
        Mockito.when(groupRepository.save(Mockito.any(Group.class))).thenAnswer(i -> i.getArgument(0));

        Group updatedGroup = groupService.joinGroupByToken("valid-token", newUser);

        assertTrue(updatedGroup.getMembers().contains(newUser));
        Mockito.verify(groupRepository, Mockito.times(1)).save(testGroup);
    }

    @Test
    public void joinGroupByToken_invalidToken_throwsException() {
        Mockito.when(groupRepository.findGroupByJoinToken("invalid-token")).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> groupService.joinGroupByToken("invalid-token", testUser));
    }

    @Test
    public void joinGroupByToken_alreadyMember_throwsException() {
        Mockito.when(groupRepository.findGroupByJoinToken("valid-token")).thenReturn(testGroup);

        assertThrows(ResponseStatusException.class, () -> groupService.joinGroupByToken("valid-token", testUser));
    }

    @Test
    public void leaveGroup_validMember_success() {
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        groupService.leaveGroup(1L, testUser);

        assertFalse(testGroup.getMembers().contains(testUser));
        Mockito.verify(groupRepository, Mockito.times(1)).save(testGroup);
    }

    @Test
    public void leaveGroup_notMember_throwsException() {
        User notMember = new User();
        notMember.setUsername("stranger");
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        assertThrows(ResponseStatusException.class, () -> groupService.leaveGroup(1L, notMember));
    }

    @Test
    public void recommendMovies_success() {
        TasteProfile profile = new TasteProfile();
        RatedMovie rm = new RatedMovie();
        rm.setMovieId("int-1");
        rm.setName("Movie A");
        rm.setRating(5.0f);
        profile.setRatedMovies(List.of(rm));
        testUser.setTasteProfile(profile);

        RecommendResponseDTO recDto = new RecommendResponseDTO();
        recDto.setMovie_id("int-2");
        recDto.setTitle("Movie B");
        recDto.setOverlap_score(0.9);

        MovieSearchResponseDTO omdbResp = new MovieSearchResponseDTO();
        MovieSearchResultDTO resultDto = new MovieSearchResultDTO();
        resultDto.setId("tt12345");
        resultDto.setPosterUrl("poster.jpg");
        omdbResp.setResults(List.of(resultDto));

        Mockito.when(movieSearchService.fetchRecommendations(Mockito.anyMap(), Mockito.eq(10), Mockito.eq(0)))
                .thenReturn(List.of(recDto));
        Mockito.when(movieSearchService.searchMovies("Movie B")).thenReturn(omdbResp);

        List<FetchedMovie> result = groupService.recommendMovies(testGroup, 0);

        assertEquals(1, result.size());
        assertEquals("Movie B", result.get(0).getName());
        assertEquals("tt12345", result.get(0).getMovieId());
        Mockito.verify(groupRepository, Mockito.times(1)).save(testGroup);
    }
    @Test
    public void getGroupsForUser_success() {
        Mockito.when(groupRepository.findByMembersContaining(testUser)).thenReturn(List.of(testGroup));
        List<Group> groups = groupService.getGroupsForUser(testUser);
        assertEquals(1, groups.size());
        assertEquals(testGroup, groups.get(0));
    }

    @Test
    public void isMember_true() {
        assertTrue(groupService.isMember(testGroup, testUser));
    }

    @Test
    public void isMember_false() {
        User outsider = new User();
        outsider.setUsername("outsider");
        assertFalse(groupService.isMember(testGroup, outsider));
    }

    @Test
    public void checkRecommendedMoviesEligibility_eligible_noException() {
        TasteProfile profile = new TasteProfile();
        RatedMovie rm = new RatedMovie();
        rm.setName("Test Movie");
        profile.setRatedMovies(List.of(rm));
        testUser.setTasteProfile(profile);

        // Should not throw an exception
        assertDoesNotThrow(() -> groupService.checkRecommendedMoviesEligibility(testGroup));
    }

    @Test
    public void checkRecommendedMoviesEligibility_ineligible_throwsException() {
        testUser.setTasteProfile(new TasteProfile()); // Empty profile
        assertThrows(ResponseStatusException.class, () -> groupService.checkRecommendedMoviesEligibility(testGroup));
    }

    @Test
    public void recommendMovies_emptyMembers_returnsEmptyList() {
        Group emptyGroup = new Group();
        emptyGroup.setId(2L);
        // Members list is null
        assertTrue(groupService.recommendMovies(emptyGroup, 0).isEmpty());
    }
}