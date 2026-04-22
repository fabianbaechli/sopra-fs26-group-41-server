package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.FetchedMovie;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.TasteProfile;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.AuthenticationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.AuthenticationPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupCreateResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupDetailsResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupSummaryDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecommendedMovieDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UsersGetDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DTOMapperTest {

    @Test
    public void convertUserPostDTOtoEntity_validInput_success() {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("alice");
        userPostDTO.setPassword("plainPassword");

        User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        assertEquals("alice", user.getUsername());
        assertEquals("plainPassword", user.getPassword());
        assertNull(user.getId());
        assertNull(user.getToken());
        assertNull(user.getStatus());
        assertNotNull(user.getTasteProfile());
        assertFalse(user.isHasLetterboxdData());
    }

    @Test
    public void convertEntityToUserGetDTO_validInput_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setStatus(UserStatus.ONLINE);

        UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

        assertEquals(1L, userGetDTO.getId());
        assertEquals("alice", userGetDTO.getUsername());
        assertEquals(UserStatus.ONLINE, userGetDTO.getStatus());
    }

    @Test
    public void convertEntityToAuthenticationGetDTO_validInput_success() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setToken("token-bob");

        AuthenticationGetDTO authenticationGetDTO =
                DTOMapper.INSTANCE.convertEntityToAuthenticationGetDTO(user);

        assertEquals(2L, authenticationGetDTO.getId());
        assertEquals("bob", authenticationGetDTO.getUsername());
        assertEquals("token-bob", authenticationGetDTO.getToken());
    }

    @Test
    public void convertEntityToAuthenticationPostDTO_validInput_success() {
        User user = new User();
        user.setUsername("charlie");
        user.setPassword("hashedPassword");

        AuthenticationPostDTO authenticationPostDTO =
                DTOMapper.INSTANCE.convertEntityToAuthenticationPostDTO(user);

        assertEquals("charlie", authenticationPostDTO.getUsername());
        assertEquals("hashedPassword", authenticationPostDTO.getPassword());
    }

    @Test
    public void convertEntityToUsersGetDTO_validInput_success() {
        TasteProfile tasteProfile = new TasteProfile();
        tasteProfile.setHighlyRatedMovies(3);

        User user = new User();
        user.setId(3L);
        user.setUsername("diana");
        user.setHasLetterboxdData(true);
        user.setTasteProfile(tasteProfile);

        UsersGetDTO usersGetDTO = DTOMapper.INSTANCE.convertEntityToUsersGetDTO(user);

        assertEquals(3L, usersGetDTO.getId());
        assertEquals("diana", usersGetDTO.getUsername());
        assertTrue(usersGetDTO.getHasLetterboxdData());

        assertNull(usersGetDTO.getTasteOverlap());
        assertNotNull(usersGetDTO.getStats());
        assertEquals(0, usersGetDTO.getStats().getMoviesLogged());
        assertEquals(3, usersGetDTO.getStats().getHighlyRatedMovies());
        assertNull(usersGetDTO.getStats().getTopGenres());
    }

    @Test
    public void convertEntityToGroupCreateResponseDTO_validInput_success() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setStatus(UserStatus.OFFLINE);

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.setOwner(owner);
        group.setProfilePicture("group-picture");
        group.setJoinToken("join-token");

        GroupCreateResponseDTO dto = DTOMapper.INSTANCE.convertEntityToGroupCreateResponseDTO(group);

        assertEquals(10L, dto.getId());
        assertEquals("Movie Night", dto.getName());
        assertEquals("group-picture", dto.getGroupProfilePicture());
        assertEquals("join-token", dto.getJoinUrl());

        assertNotNull(dto.getOwner());
        assertEquals(1L, dto.getOwner().getId());
        assertEquals("owner", dto.getOwner().getUsername());
        assertEquals(UserStatus.OFFLINE, dto.getOwner().getStatus());
    }

    @Test
    public void convertEntityToGroupSummaryDTO_withMembers_success() {
        User member1 = new User();
        member1.setUsername("alice");

        User member2 = new User();
        member2.setUsername("bob");

        Group group = new Group();
        group.setId(20L);
        group.setGroupName("Weekend Picks");
        group.addMember(member1);
        group.addMember(member2);

        GroupSummaryDTO dto = DTOMapper.INSTANCE.convertEntityToGroupSummaryDTO(group);

        assertEquals(20L, dto.getId());
        assertEquals("Weekend Picks", dto.getName());
        assertNull(dto.getGroupProfilePicture());
        assertEquals(2, dto.getMemberCount());
    }

    @Test
    public void convertEntityToGroupSummaryDTO_withoutMembers_success() {
        Group group = new Group();
        group.setId(21L);
        group.setGroupName("Empty Group");

        GroupSummaryDTO dto = DTOMapper.INSTANCE.convertEntityToGroupSummaryDTO(group);

        assertEquals(21L, dto.getId());
        assertEquals("Empty Group", dto.getName());
        assertEquals(0, dto.getMemberCount());
    }

    @Test
    public void convertEntityToGroupDetailsResponseDTO_validInput_success() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");

        User member1 = new User();
        member1.setId(2L);
        member1.setUsername("alice");

        User member2 = new User();
        member2.setId(3L);
        member2.setUsername("bob");

        Group group = new Group();
        group.setId(30L);
        group.setGroupName("Drama Club");
        group.setOwner(owner);
        group.setJoinToken("group-token");
        group.addMember(member1);
        group.addMember(member2);

        GroupDetailsResponseDTO dto = DTOMapper.INSTANCE.convertEntityToGroupDetailsResponseDTO(group);

        assertEquals(30L, dto.getId());
        assertEquals("Drama Club", dto.getName());
        assertEquals(1L, dto.getOwnerId());
        assertEquals("group-token", dto.getJoinUrl());
        assertNull(dto.getGroupProfilePicture());

        assertNotNull(dto.getMembers());
        assertEquals(2, dto.getMembers().size());
        assertEquals(2L, dto.getMembers().get(0).getId());
        assertEquals("alice", dto.getMembers().get(0).getUsername());
        assertEquals(3L, dto.getMembers().get(1).getId());
        assertEquals("bob", dto.getMembers().get(1).getUsername());
    }

    @Test
    public void convertEntityToGroupDetailsResponseDTO_withoutOwnerOrMembers_success() {
        Group group = new Group();
        group.setId(31L);
        group.setGroupName("No Members Yet");
        group.setJoinToken("empty-token");

        GroupDetailsResponseDTO dto = DTOMapper.INSTANCE.convertEntityToGroupDetailsResponseDTO(group);

        assertEquals(31L, dto.getId());
        assertEquals("No Members Yet", dto.getName());
        assertNull(dto.getOwnerId());
        assertEquals("empty-token", dto.getJoinUrl());
        assertNull(dto.getGroupProfilePicture());
        assertNotNull(dto.getMembers());
        assertTrue(dto.getMembers().isEmpty());
    }

    @Test
    public void convertEntityToRecommendedMovieDTO_withOverlapScore_success() {
        FetchedMovie fetchedMovie = new FetchedMovie();
        fetchedMovie.setMovieId("tt0111161");
        fetchedMovie.setName("The Shawshank Redemption");
        fetchedMovie.setPosterUrl("poster-url");
        fetchedMovie.setOverlapScore(0.87);

        RecommendedMovieDTO dto = DTOMapper.INSTANCE.convertEntityToRecommendedMovieDTO(fetchedMovie);

        assertEquals("tt0111161", dto.getMovieId());
        assertEquals("The Shawshank Redemption", dto.getTitle());
        assertEquals("poster-url", dto.getPosterUrl());
        assertEquals(87, dto.getGroupMatchScore());
    }

    @Test
    public void convertEntityToRecommendedMovieDTO_withoutOverlapScore_success() {
        FetchedMovie fetchedMovie = new FetchedMovie();
        fetchedMovie.setMovieId("tt0068646");
        fetchedMovie.setName("The Godfather");
        fetchedMovie.setPosterUrl("poster-url");
        fetchedMovie.setOverlapScore(null);

        RecommendedMovieDTO dto = DTOMapper.INSTANCE.convertEntityToRecommendedMovieDTO(fetchedMovie);

        assertEquals("tt0068646", dto.getMovieId());
        assertEquals("The Godfather", dto.getTitle());
        assertEquals("poster-url", dto.getPosterUrl());
        assertEquals(0, dto.getGroupMatchScore());
    }
}