import cluster.management.OnElectionCallback;
import cluster.management.ServiceRegistry;
import networking.WebClient;
import networking.WebServer;
import org.apache.zookeeper.KeeperException;
import search.SearchCoordinator;
import search.SearchWorker;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry workersServiceRegistry;
    private final ServiceRegistry coordinatorServiceRegistry;

    private WebServer webServer;
    private int port;

    public OnElectionAction(ServiceRegistry workersServiceRegistry, ServiceRegistry coordinatorServiceRegistry, int port) {
        this.workersServiceRegistry = workersServiceRegistry;
        this.coordinatorServiceRegistry = coordinatorServiceRegistry;
        this.port = port;
    }

    @Override
    public void onElectedToBeLeader() throws InterruptedException, KeeperException {
        workersServiceRegistry.unregisterFromCluster();
        workersServiceRegistry.registerForUpdates();
        if(webServer != null){
            webServer.stop();
        }
        SearchCoordinator searchCoordinator = new SearchCoordinator(workersServiceRegistry, new WebClient());

        webServer = new WebServer(port, searchCoordinator);
        webServer.startServer();
        try {
            String currentServerAddress = String.format("http://%s:%d%s", Inet4Address.getLocalHost().getCanonicalHostName(), port, searchCoordinator.getEndpoint());
            coordinatorServiceRegistry.registerToCluster(currentServerAddress);
        } catch (InterruptedException e) {
        } catch (KeeperException | UnknownHostException e) {
        }
    }

    @Override
    public void onWorker() {
        SearchWorker searchWorker = new SearchWorker();
        if(webServer == null){
            webServer = new WebServer(port, searchWorker);
            webServer.startServer();
        }
        try{
            String currentServerAddress = String.format("http://%s:%d%s", InetAddress.getLocalHost().getCanonicalHostName(), port, searchWorker.getEndpoint());
            workersServiceRegistry.registerToCluster(currentServerAddress);
        } catch (UnknownHostException e) {
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }
    }
}
