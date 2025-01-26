package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    private final ZooKeeper zooKeeper;

    public static final String WORKER_SERVICE_REGISTRY = "/worker_service_registry";
    public static final String COORDINATOR_SERVICE_REGISTRY = "/coordinator_service_registry";

    private String currentZnode = null;
    private List<String> allServiceAddresses = null;

    private String serviceRegistryZnode = null;
    public ServiceRegistry(ZooKeeper zooKeeper, String serviceRegistryZnode){
        this.zooKeeper = zooKeeper;
        this.serviceRegistryZnode = serviceRegistryZnode;
        createServiceRegistry();
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {

        if(this.currentZnode != null){
            System.out.println("Already registered to the service registry!");
            return;
        }
        this.currentZnode = zooKeeper.create(serviceRegistryZnode + "/n_", metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to service registry successfully");
    }


    private void createServiceRegistry() {
        try {
            if(zooKeeper.exists(serviceRegistryZnode, false) == null){
                zooKeeper.create(serviceRegistryZnode, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void registerForUpdates(){
        try{
            updateAddresses();
        }catch (KeeperException e){

        }catch (InterruptedException e){

        }
    }

    public synchronized List<String> getAllServiceAddresses() throws KeeperException, InterruptedException{
        if(allServiceAddresses == null){
            updateAddresses();
        }
        return allServiceAddresses;
    }

    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        if(currentZnode != null && zooKeeper.exists(currentZnode, false) != null){
            zooKeeper.delete(currentZnode, -1);
        }
    }
    private synchronized void updateAddresses() throws InterruptedException, KeeperException {
        List<String> workerZnodes = zooKeeper.getChildren(serviceRegistryZnode, this);
        List<String> addresses = new ArrayList<>(workerZnodes.size());
        for(String workerZnode: workerZnodes){
            String workerZnodeFullPath = serviceRegistryZnode + "/" + workerZnode;
            Stat stat = zooKeeper.exists(workerZnodeFullPath, false);
            if(stat == null){
                continue;
            }
            byte [] addressBytes = zooKeeper.getData(workerZnodeFullPath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }

        this.allServiceAddresses = Collections.unmodifiableList(addresses);
        System.out.println("The cluster address are :" + this.allServiceAddresses);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        try{
            updateAddresses();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
