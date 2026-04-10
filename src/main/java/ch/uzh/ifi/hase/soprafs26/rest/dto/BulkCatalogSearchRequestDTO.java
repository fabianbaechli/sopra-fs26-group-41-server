package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class BulkCatalogSearchRequestDTO {
    private List<String> names;

    public BulkCatalogSearchRequestDTO() {
    }

    public BulkCatalogSearchRequestDTO(List<String> names) {
        this.names = names;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }
}