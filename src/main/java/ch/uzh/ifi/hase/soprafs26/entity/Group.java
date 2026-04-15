package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "groups")
public class Group implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String groupName;

    @ManyToMany
    private List<User> members;

    @Embedded
    private TasteProfile groupTasteProfile = new TasteProfile();

    @ElementCollection(fetch = FetchType.EAGER)
    private List<FetchedMovie> recommendedMovies = new ArrayList<>();

    //private Poll Poll;

    private String joinToken;

    // todo: will change to a canvas object in future => not yet implemented
    private String profilePicture;
    @ManyToOne
    private User owner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<User> getMembers() {
        return members;
    }

    public void addMember(User member) {
        // Initialize the list if it's the first member being added
        if (this.members == null) {
            this.members = new java.util.ArrayList<>();
        }
        // Prevent duplicate entries at the entity level
        if (!this.members.contains(member)) {
            this.members.add(member);
        }
    }

    public void removeMember(User member) {
        if (this.members != null) {
            this.members.remove(member);
        }
    }

    public TasteProfile getGroupTasteProfile() {
        return groupTasteProfile;
    }

    public void setGroupTasteProfile(TasteProfile groupTasteProfile) {
        this.groupTasteProfile = groupTasteProfile;
    }

    public String getJoinToken() {
        return joinToken;
    }

    public void setJoinToken(String inviteLink) {
        this.joinToken = inviteLink;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public List<FetchedMovie> getRecommendedMovies() {return recommendedMovies;}

    public void setRecommendedMovies(List<FetchedMovie> recommendedMovies) {this.recommendedMovies = recommendedMovies;}

}