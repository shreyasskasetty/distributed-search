package model;

import java.util.Map;

public class DocumentData {

    private Map<String, Double> termFrequencyMap;
    public void addTermFrequency(String term, double frequency){
       this.termFrequencyMap.put(term, frequency);
    }
    public Double getTermFrequency(String term) {
       return this.termFrequencyMap.getOrDefault(term, 0.0);
    }
}
