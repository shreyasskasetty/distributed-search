package search;

import cluster.management.ServiceRegistry;
import networking.OnRequestCallback;
import networking.WebClient;

public class SearchCoordinator implements OnRequestCallback {
    private final ServiceRegistry workersServiceRegistry;
    private final WebClient webClient;

    public SearchCoordinator(ServiceRegistry workersServiceRegistry, WebClient webClient){
        this.workersServiceRegistry = workersServiceRegistry;
        this.webClient = webClient;
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        return new byte[0];
    }

    @Override
    public String getEndpoint() {
        return null;
    }
}
