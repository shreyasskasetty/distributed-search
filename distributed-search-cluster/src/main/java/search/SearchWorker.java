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
    public final static String WORKER_ENDPOINT = "/task";

    private Result createResult(Task task){
        Result result = new Result();
        List<String> documents = task.getDocuments();
        System.out.println(String.format("Received %d documents to process", documents.size()));
        for(String document: documents){
            List<String> words = parseWordsFromDocument(document);
            DocumentData documentData = TFIDF.createDocumentData(task.getSearchTerms(), words);
            result.addDocumentData(document, documentData);
        }
        return result;
    }

    private List<String> parseWordsFromDocument(String document){
        FileReader fileReader = null;
        try{
            fileReader = new FileReader(document);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = bufferedReader.lines().collect(Collectors.toList());
        return TFIDF.getWordsFromDocument(lines);
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        Task task = (Task) SerializationUtils.deserialize(requestPayload);
        try {
            Result result = createResult(task);
            return SerializationUtils.serialize(result);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public String getEndpoint() {
        return WORKER_ENDPOINT;
    }
}
