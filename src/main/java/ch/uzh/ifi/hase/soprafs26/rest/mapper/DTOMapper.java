package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.User;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

    DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "status", target = "status")
    UserGetDTO convertEntityToUserGetDTO(User user);

    @Mapping(source = "token", target = "token")
    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    AuthenticationGetDTO convertEntityToAuthenticationGetDTO(User user);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
    AuthenticationPostDTO convertEntityToAuthenticationPostDTO(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "hasLetterboxdData", target = "hasLetterboxdData")
    //Will be commented out once we have the weighted graph implemented and are able to calculate it
    //@Mapping(source = "tasteOverlap", target = "tasteOverlap")
    @Mapping(target = "tasteOverlap", ignore = true)
    @Mapping(source = "tasteProfile.moviesLogged", target = "stats.moviesLogged")
    @Mapping(source = "tasteProfile.highlyRatedMovies", target = "stats.highlyRatedMovies")
    //Commented out until we build a class that fetches the data from OMDB without hitting the rate limit
    //@Mapping(source = "topGenres", target = "stats.topGenres")
    @Mapping(target = "stats.topGenres", ignore = true)
    UsersGetDTO convertEntityToUsersGetDTO(User user);
}
