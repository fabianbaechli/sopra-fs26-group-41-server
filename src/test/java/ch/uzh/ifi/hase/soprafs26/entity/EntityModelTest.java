package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityModelTest {

    @Test
    void group_addMember_initializesListAndPreventsDuplicates() {
        Group group = new Group();

        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        group.addMember(alice);
        group.addMember(alice);

        assertNotNull(group.getMembers());
        assertEquals(1, group.getMembers().size());
        assertSame(alice, group.getMembers().get(0));
    }

    @Test
    void group_removeMember_removesExistingMemberAndIgnoresNullList() {
        Group emptyGroup = new Group();
        User alice = new User();

        assertDoesNotThrow(() -> emptyGroup.removeMember(alice));

        Group group = new Group();
        group.addMember(alice);
        group.removeMember(alice);

        assertTrue(group.getMembers().isEmpty());
    }

    @Test
    void group_gettersAndSetters_roundTrip() {
        Group group = new Group();

        User owner = new User();
        owner.setUsername("owner");

        TasteProfile profile = new TasteProfile();
        FetchedMovie movie = new FetchedMovie();
        movie.setMovieId("tt0111161");

        byte[] profilePicture = "image".getBytes();

        group.setId(10L);
        group.setGroupName("Movie Club");
        group.setOwner(owner);
        group.setJoinToken("join-token");
        group.setGroupTasteProfile(profile);
        group.setProfilePicture(profilePicture);
        group.setRecommendedMovies(List.of(movie));

        assertEquals(10L, group.getId());
        assertEquals("Movie Club", group.getGroupName());
        assertSame(owner, group.getOwner());
        assertEquals("join-token", group.getJoinToken());
        assertSame(profile, group.getGroupTasteProfile());
        assertArrayEquals(profilePicture, group.getProfilePicture());
        assertEquals(List.of(movie), group.getRecommendedMovies());
    }

    @Test
    void user_setTasteProfileNull_createsNewTasteProfile() {
        User user = new User();

        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("password");
        user.setToken("token");
        user.setStatus(UserStatus.ONLINE);
        user.setHasLetterboxdData(true);
        user.setTasteProfile(null);

        assertEquals(1L, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("password", user.getPassword());
        assertEquals("token", user.getToken());
        assertEquals(UserStatus.ONLINE, user.getStatus());
        assertTrue(user.isHasLetterboxdData());
        assertNotNull(user.getTasteProfile());
    }

    @Test
    void tasteProfile_setRatedMoviesNull_usesEmptyListAndMoviesLoggedIsZero() {
        TasteProfile profile = new TasteProfile();

        profile.setRatedMovies(null);

        assertNotNull(profile.getRatedMovies());
        assertTrue(profile.getRatedMovies().isEmpty());
        assertEquals(0, profile.getMoviesLogged());
    }

    @Test
    void tasteProfile_mergeTasteProfiles_deduplicatesByMovieNameAndCountsHighlyRatedMovies() {
        RatedMovie movie1 = new RatedMovie();
        movie1.setMovieId("tt1");
        movie1.setName("Movie A");
        movie1.setRating(5.0f);
        movie1.setYear(2001);

        RatedMovie duplicateMovie = new RatedMovie();
        duplicateMovie.setMovieId("tt1b");
        duplicateMovie.setName("Movie A");
        duplicateMovie.setRating(4.0f);

        RatedMovie movie2 = new RatedMovie();
        movie2.setMovieId("tt2");
        movie2.setName("Movie B");
        movie2.setRating(4.5f);

        TasteProfile profile1 = new TasteProfile();
        profile1.setRatedMovies(List.of(movie1, duplicateMovie));

        TasteProfile profile2 = new TasteProfile();
        profile2.setRatedMovies(List.of(movie2));

        TasteProfile merged = TasteProfile.MergeTasteProfiles(List.of(profile1, profile2));

        assertEquals(2, merged.getRatedMovies().size());
        assertEquals(1, merged.getHighlyRatedMovies());
        assertEquals(2, merged.getMoviesLogged());
    }

    @Test
    void fetchedMovie_gettersAndSetters_roundTrip() {
        FetchedMovie movie = new FetchedMovie();

        movie.setMovieId("tt123");
        movie.setName("Chungking Express");
        movie.setInternalId("internal-123");
        movie.setOverlapScore(0.75);
        movie.setPosterUrl("poster-url");

        assertEquals("tt123", movie.getMovieId());
        assertEquals("Chungking Express", movie.getName());
        assertEquals("internal-123", movie.getInternalId());
        assertEquals(0.75, movie.getOverlapScore());
        assertEquals("poster-url", movie.getPosterUrl());
    }

    @Test
    void ratedMovie_gettersAndSetters_roundTrip() {
        RatedMovie movie = new RatedMovie();

        movie.setMovieId("tt123");
        movie.setName("A fish called Wanda");
        movie.setRating(4.5f);
        movie.setYear(2010);

        assertEquals("tt123", movie.getMovieId());
        assertEquals("A fish called Wanda", movie.getName());
        assertEquals(4.5f, movie.getRating());
        assertEquals(2010, movie.getYear());
    }
}