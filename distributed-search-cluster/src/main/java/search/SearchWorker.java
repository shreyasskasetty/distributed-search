package search;

import model.DocumentData;
import model.Result;
import model.Task;
import networking.OnRequestCallback;
import utils.SerializationUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SearchWorker implements OnRequestCallback {
    private final String WORKER_ENDPOINT = "/task";

    private Result createResult(List<String> searchTerms, List<String> documents){
        Result result = new Result();
        for(String document: documents){
            List<String> words = parseWordsFromDocument(document);
            DocumentData documentData = TFIDF.createDocumentData(searchTerms, words);
            result.addDocumentData(document, documentData);
        }
        return result;
    }

    private List<String> parseWordsFromDocument(String document){
        FileReader fileReader = null;
        try{
            fileReader = new FileReader(document);
        } catch (FileNotFoundException e) {
            return Collections.emptyList();
        }
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = bufferedReader.lines().collect(Collectors.toList());
        List<String> words = TFIDF.getWordsFromDocument(lines);
        return words;
    }
    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        Task task = (Task) SerializationUtils.deserialize(requestPayload);
        if(task != null){
            Result result = createResult(task.getSearchTerms(), task.getDocuments());
            return SerializationUtils.serialize(result);
        }
        return new byte[0];
    }

    @Override
    public String getEndpoint() {
        return WORKER_ENDPOINT;
    }
}
