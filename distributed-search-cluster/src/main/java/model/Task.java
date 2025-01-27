package model;

import java.io.Serializable;
import java.util.List;

public class Task implements Serializable {
    private List<String> documents;
    private List<String> searchTerms;

    public Task(List<String> searchTerms, List<String> documents) {
        this.searchTerms = searchTerms;
        this.documents = documents;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

}
