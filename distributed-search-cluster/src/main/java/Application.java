import cluster.management.LeaderElection;
import cluster.management.ServiceRegistry;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;


public class Application implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 8080;
    private ZooKeeper zooKeeper;

    public ZooKeeper connectedToZookeeper() throws IOException{
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return zooKeeper;
    }

    public void run() throws InterruptedException{
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException{
        zooKeeper.close();
    }

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectedToZookeeper();
        LeaderElection leaderElection = getLeaderElection(zooKeeper, currentServerPort);
        leaderElection.reelectLeader();
        application.run();
        application.close();
        System.out.println("Disconnected from Zookeeper, exiting application");
    }

    private static LeaderElection getLeaderElection(ZooKeeper zooKeeper, int currentServerPort) throws InterruptedException, KeeperException {
        ServiceRegistry workerServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.WORKER_SERVICE_REGISTRY);
        ServiceRegistry coordinatorServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.COORDINATOR_SERVICE_REGISTRY);
        OnElectionAction onElectionCallback = new OnElectionAction(workerServiceRegistry, coordinatorServiceRegistry, currentServerPort);
        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionCallback);
        leaderElection.volunteerForLeadership();
        return leaderElection;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()){
            case None:
                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    System.out.println("Successfully connected to Zookeeper");
                } else{
                    synchronized (zooKeeper){
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            default:
                break;
        }
    }
}