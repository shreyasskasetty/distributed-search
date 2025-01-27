package model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentData implements Serializable {

    private Map<String, Double> termFrequencyMap = new HashMap<>();;
    public void addTermFrequency(String term, double frequency){
       this.termFrequencyMap.put(term, frequency);
    }
    public Double getTermFrequency(String term) {
       return this.termFrequencyMap.getOrDefault(term, 0.0);
    }
}
