package server.zoo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ldr.server.serialization.my.DataEncoder;
import ldr.server.serialization.my.StringEncoder;

import static server.zoo.ZooUtils.getDelta;

@Service
public class WorkersZoo implements IWorkersZoo, Watcher {
    private static final Logger log = LoggerFactory.getLogger(WorkersZoo.class);

    private final List<IWorkersSubscriber> subscribers = new ArrayList<>();
    private final ZooKeeper zooKeeper;
    private final String workersZNode;
    // Init to avoid npe in updateWorkers for the first time.
    private Set<String> prevWorkers = new HashSet<>();
    private Set<String> workers = new HashSet<>();
    DataEncoder<String> hostCoder = new StringEncoder();

    public WorkersZoo(ZooKeeper zooKeeper, @Value("${workers.zoo.znode}") String workersZNode) {
        this.zooKeeper = zooKeeper;
        this.workersZNode = workersZNode;
        updateWorkers();
    }

    @Override
    public void subscribe(IWorkersSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Host names are resolved from zookeeper every time, so don't attempt to use this method very often.
     */
    @Override
    public List<WorkerInfo> getWorkers() {
        List<WorkerInfo> result = new ArrayList<>(workers.size());
        for (String name : workers) {
            try {
                result.add(getWorker(name));
            } catch (InterruptedException | KeeperException e) {
                log.error("Can't resolve worker during getWorkers: {}", name, e);
            }
        }

        return result;
    }

    @Override
    public void addWorker(WorkerInfo worker) {
        byte[] hostBytes = hostCoder.encode(worker.host());
        try {
            zooKeeper.create(workersZNode + "/" + worker.name(), hostBytes,
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            log.error("Can't add worker: {}", worker.name(), e);
        }
    }

    @Override
    public synchronized void process(WatchedEvent event) {
        if (event.getPath().equals(workersZNode) &&
                event.getType() == Event.EventType.NodeChildrenChanged) {
            if (updateWorkers()) {
                notifySubscribers();
            }
        } else {
            log.error("Unsupported event: {}", event);
        }
    }

    private boolean updateWorkers() {
        prevWorkers = workers;
        try {
            workers = new HashSet<>(zooKeeper.getChildren(workersZNode, this));
            log.info("Workers updated: {}", workers);
        } catch (KeeperException | InterruptedException e) {
            log.error("Can't update workers.", e);
            return false;
        }

        return true;
    }

    private void notifySubscribers() {
        if (workers.size() > prevWorkers.size()) {
            String newWorkerName = getDelta(workers, prevWorkers);
            WorkerInfo newWorker;
            try {
                newWorker = getWorker(newWorkerName);
            } catch (InterruptedException | KeeperException e) {
                log.error("Can't resolve worker {} during notifySubscribers", newWorkerName, e);
                return;
            }

            for (var sub : subscribers) {
                // TODO: Get data with host from this znode
                sub.newWorker(newWorker);
            }

            log.info("Subscribers notified about new worker: {}", newWorkerName);
        } else if (workers.size() < prevWorkers.size()) {
            String deletedWorker = getDelta(prevWorkers, workers);
            for (var sub : subscribers) {
                sub.deletedWorker(deletedWorker);
            }

            log.info("Subscribers notified about deleted worker: {}", deletedWorker);
        } else {
            log.error("Attempt to notify subscribers but workers didn't change. Workers: {}. Previous workers: {}",
                    workers, prevWorkers);
        }
    }

    /**
     * Resolve host from zookeeper and construct new worker.
     */
    private WorkerInfo getWorker(String name) throws InterruptedException, KeeperException {
        // TODO: Check that path is correct
        byte[] hostBytes = zooKeeper.getData(workersZNode + "/" + name, false, null);
        return new WorkerInfo(name, hostCoder.decode(hostBytes).result());
    }
}

