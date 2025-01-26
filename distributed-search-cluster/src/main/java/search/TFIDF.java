package search;

import model.DocumentData;
import model.Result;

import javax.print.Doc;
import java.util.*;


public class TFIDF {
    public static DocumentData createDocumentData(List<String> terms, List<String> words){
        DocumentData documentData = new DocumentData();
        for(String term : terms){
            documentData.addTermFrequency(term, TFIDF.calculateTermFrequency(term, words));
        }
        return documentData;
    }

    private static Double calculateTermFrequency(String term, List<String> words){
        int count = 0;

        // Calculate term frequency by checking if the term is equal to words in the word list.
        // Note: Make sure to have both the word and term in lowercase when comparing
        for(String word: words){
            if(word.equalsIgnoreCase(term.toLowerCase())){
                count += 1;
            }
        }
        double termFrequency = (double) count / words.size();
        return termFrequency;
    }

    //Will be used in Coordinator node
    private static Map<String, Double> getTermToIDFMap(List<String> terms, Map<String, DocumentData> documentToDocumentData){
        Map<String, Double> termToIDFMap = new HashMap<>();
        for(String term: terms){
            Double inverseDocumentFrequency = calculateInverseDocumentFrequency(term, documentToDocumentData);
            termToIDFMap.put(term, inverseDocumentFrequency);
        }
        return termToIDFMap;
    }

    private static Double calculateInverseDocumentFrequency(String term, Map<String, DocumentData> documentToDocumentData){
        int count = 0;
        for(Map.Entry<String, DocumentData> documentNameDocumentDataEntry : documentToDocumentData.entrySet()){
            DocumentData documentData = documentNameDocumentDataEntry.getValue();
            if(documentData.getTermFrequency(term) > 0.0){
                count++;
            }
        }
        return count == 0?0:Math.log10((double) documentToDocumentData.size()/count);
    }

    public static Map<Double, List<String>> getDocumentScores(List<String> terms,  Map<String, DocumentData> documentToDocumentData){
        TreeMap<Double, List<String>> documentScores = new TreeMap<>();
        Map<String, Double> termToIDFMap = getTermToIDFMap(terms, documentToDocumentData);
        for(String document: documentToDocumentData.keySet()){
            DocumentData documentData = documentToDocumentData.get(document);
            Double documentScore = calculateTFIDFScore(terms, documentData, termToIDFMap);
            List<String> documentList = documentScores.getOrDefault(documentScore, new ArrayList<>());
            documentList.add(document);
            documentScores.put(documentScore, documentList);
        }
        return documentScores;
    }

    private static double calculateTFIDFScore(List<String> terms, DocumentData documentData, Map<String, Double> termToIDFMap){
        double score = 0.0;
        for(String term: terms){
            score  += documentData.getTermFrequency(term) * termToIDFMap.get(term);
        }
        return score;
    }

    public static List<String> getWordsFromDocument(List<String> lines){
        List<String> wordsList = new ArrayList<>();
        for(String line : lines){
            wordsList.addAll(getWordsFromLine(line));
        }
        return wordsList;
    }

    private static List<String> getWordsFromLine(String line){
        return Arrays.asList(line.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(/d)+|(/n)+"));
    }
}
