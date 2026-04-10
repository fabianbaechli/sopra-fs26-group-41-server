package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class BulkCatalogSearchResultDTO {
    private String query;
    private CatalogMovieDTO[] matches;

    public BulkCatalogSearchResultDTO() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public CatalogMovieDTO[] getMatches() {
        return matches;
    }

    public void setMatches(CatalogMovieDTO[] matches) {
        this.matches = matches;
    }
}