package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;

import java.io.Serializable;
import java.util.*;

@Embeddable
public class TasteProfile implements Serializable {

    @ElementCollection(fetch = FetchType.EAGER)
    private List<RatedMovie> ratedMovies = new ArrayList<>();
    private int highlyRatedMovies=0;

    public List<RatedMovie> getRatedMovies() {
        return ratedMovies;
    }

    public void setRatedMovies(List<RatedMovie> ratedMovies) {
        this.ratedMovies = ratedMovies != null ? ratedMovies : new ArrayList<>();
    }

    public int getHighlyRatedMovies() {
        return highlyRatedMovies;
    }
    public void setHighlyRatedMovies(int highlyRatedMovies) {
        this.highlyRatedMovies = highlyRatedMovies;
    }
    private void addRatedMovies(List<RatedMovie> ratedMovies) {
        this.ratedMovies.addAll(ratedMovies);
    }
    public static TasteProfile MergeTasteProfiles(List<TasteProfile> tasteProfiles) {
        TasteProfile mergedProfile = new TasteProfile();
        Map<String,RatedMovie> ratedMoviesHashMap=new LinkedHashMap<>();
        tasteProfiles.forEach(tasteProfile->{
            if(tasteProfile.getRatedMovies()!=null){
                for(RatedMovie ratedMovie:tasteProfile.getRatedMovies()){
                    ratedMoviesHashMap.put(ratedMovie.getName(),ratedMovie);
                }
            }
        });
        mergedProfile.setRatedMovies(new ArrayList<>(ratedMoviesHashMap.values()));
        int count = (int) mergedProfile.getRatedMovies().stream()
                .filter(m -> m.getRating() >= 4.5)
                .count();
        mergedProfile.setHighlyRatedMovies(count);

        return mergedProfile;
    }
    public int getMoviesLogged() {
        if (this.ratedMovies == null) {
            return 0;
        }
        return this.ratedMovies.size();
    }
}
