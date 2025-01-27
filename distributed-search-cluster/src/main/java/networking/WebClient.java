package networking;

import model.Result;
import utils.SerializationUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WebClient {
    private HttpClient client;

    public WebClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public CompletableFuture<Result> sendTask(String url, byte[] requestPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestPayload))
                .uri(URI.create(url))
                .build();
        System.out.println(url);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(httpResponse -> {
                    System.out.println(httpResponse.statusCode());
                    return (Result) SerializationUtils.deserialize(httpResponse.body());
                });
    }
}
