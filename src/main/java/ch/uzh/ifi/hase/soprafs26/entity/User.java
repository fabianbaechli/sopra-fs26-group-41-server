package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

import java.io.Serializable;
import java.util.Objects;

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
@Table(name = "users")
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
	private String token;

	@Column(nullable = false)
	private UserStatus status;

    @Embedded
    private TasteProfile tasteProfile = new TasteProfile();

    private boolean hasLetterboxdData=true;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public TasteProfile getTasteProfile() {
        this.setHasLetterboxdData(true);
        return tasteProfile;
    }

    public void setTasteProfile(TasteProfile tasteProfile) {
        this.tasteProfile = Objects.requireNonNullElseGet(tasteProfile, TasteProfile::new);
    }
    public boolean isHasLetterboxdData() {
        return hasLetterboxdData;
    }
    public void setHasLetterboxdData(boolean hasLetterboxdData) {
        this.hasLetterboxdData = hasLetterboxdData;
    }

}
