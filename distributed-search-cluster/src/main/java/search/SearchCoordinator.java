package search;

import cluster.management.ServiceRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import model.DocumentData;
import model.Result;
import model.Task;
import model.proto.SearchModel;
import networking.OnRequestCallback;
import networking.WebClient;
import org.apache.zookeeper.KeeperException;
import utils.SerializationUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchCoordinator implements OnRequestCallback {
    private final ServiceRegistry workersServiceRegistry;
    private final WebClient client;

    private static final String BOOKS_DIRECTORY = "./resources/books/";
    private static final String COORDINATOR_ENDPOINT= "/search";
    private final List<String> documents;

    public SearchCoordinator(ServiceRegistry workersServiceRegistry, WebClient webClient){
        this.workersServiceRegistry = workersServiceRegistry;
        this.client = webClient;
        this.documents = getDocumentsList();
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        try {
            SearchModel.Request request = SearchModel.Request.parseFrom(requestPayload);
            SearchModel.Response response = createResponse(request);
            return response.toByteArray();
        } catch (InvalidProtocolBufferException | InterruptedException | KeeperException e) {
            e.printStackTrace();
            return SearchModel.Response.getDefaultInstance().toByteArray();
        }
    }

    private List<SearchModel.Response.DocumentStats> aggregateResults(List<Result> results, List<String> searchTerms){

        Map<String, DocumentData> allDocumentsResults = new HashMap<>();

        for (Result result : results) {
            allDocumentsResults.putAll(result.getDocumentToDocumentDataMap());
        }

        System.out.println("Calculating score for all the documents");
        Map<Double, List<String>> scoreToDocuments = TFIDF.getDocumentScores(searchTerms, allDocumentsResults);


        return getSortedDocumentStats(scoreToDocuments);
    }

    private List<SearchModel.Response.DocumentStats> getSortedDocumentStats(Map<Double, List<String>> documentScores){
        List<SearchModel.Response.DocumentStats> sortedDocumentStats = new ArrayList<>();
        for(Map.Entry<Double, List<String>> scoreEntry: documentScores.entrySet()){
            Double score = scoreEntry.getKey();
            for(String document: scoreEntry.getValue()){
                File file  = new File(document);
                sortedDocumentStats.add(SearchModel.Response.DocumentStats.newBuilder()
                        .setScore(score)
                        .setDocumentName(file.getName())
                        .setDocumentSize(file.length())
                        .build());
            }
        }
        return sortedDocumentStats;
    }

    private SearchModel.Response createResponse(SearchModel.Request request) throws InterruptedException, KeeperException {
        SearchModel.Response.Builder searchResponse = SearchModel.Response.newBuilder();

        System.out.println("Receieved the search query: " + request.getSearchQuery());

        List<String> searchTerms = getSearchTerms(request.getSearchQuery());
        List<String> workers = this.workersServiceRegistry.getAllServiceAddresses();

        if(workers.isEmpty()) {
            System.out.println("No workers available!");
            return searchResponse.build();
        }

        List<Task> tasks = createTasks(workers.size(), searchTerms);

        List<Result> results = sendTasksToWorkers(workers, tasks);

        List<SearchModel.Response.DocumentStats> aggregatedResults = aggregateResults(results, searchTerms);

        searchResponse.addAllRelevantDocuments(aggregatedResults);

        return searchResponse.build();
    }

    private List<String> getSearchTerms(String searchQuery){
        return TFIDF.getWordsFromLine(searchQuery);
    }

    private List<Result> sendTasksToWorkers(List<String> workers, List<Task> tasks) {
        CompletableFuture<Result>[] futures = new CompletableFuture[workers.size()];
        for (int i = 0; i < workers.size(); i++) {
            String worker = workers.get(i);
            Task task = tasks.get(i);
            byte[] payload = SerializationUtils.serialize(task);

            futures[i] = client.sendTask(worker, payload);
        }

        List<Result> results = new ArrayList<>();
        for (CompletableFuture<Result> future : futures) {
            try {
                Result result = future.get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println(String.format("Received %d/%d results", results.size(), tasks.size()));
        return results;
    }

    private List<Task> createTasks(int numOfWorkers, List<String> searchTerms){
        List<Task> tasks = new ArrayList<>();
        List<List<String>> documentsForWorkers = splitDocumentsForWorkers(numOfWorkers);
        for(List<String> documents: documentsForWorkers){
            Task task = new Task(searchTerms, documents);
            tasks.add(task);
        }
        return tasks;
    }

    private List<List<String>> splitDocumentsForWorkers(int numOfWorkers){
        List<List<String>> documentsForWorkers = new ArrayList<>();
        int documentsPerWorker = (documents.size()  + numOfWorkers - 1)/numOfWorkers;
        for(int i = 0; i < numOfWorkers; i++){
           int startIndex = i * documentsPerWorker;
           int endIndex = Math.min(startIndex + documentsPerWorker, documents.size());
           if(startIndex >= endIndex){
               break;
           }
           List<String> documentsForWorker = new ArrayList<>(documents.subList(startIndex, endIndex));
           documentsForWorkers.add(documentsForWorker);
        }
        return documentsForWorkers;
    }

    private List<String> getDocumentsList(){
        File documentsDirectory = new File(BOOKS_DIRECTORY);;
        return Stream.of(Objects.requireNonNull(documentsDirectory.list()))
                .map(documentName -> BOOKS_DIRECTORY + documentName).collect(Collectors.toList());
    }

    @Override
    public String getEndpoint() {
        return COORDINATOR_ENDPOINT;
    }
}
