package server.zoo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkersZoo implements IWorkersZoo, Watcher {
    private static final Logger log = LoggerFactory.getLogger(WorkersZoo.class);

    private final List<Consumer<List<String>>> subscribers = new ArrayList<>();
    private final ZooKeeper zooKeeper;
    private final String workersZNode;
    private List<String> workers;

    public WorkersZoo(ZooKeeper zooKeeper, @Value("${workerzoo.znode}") String workersZNode) {
        this.zooKeeper = zooKeeper;
        this.workersZNode = workersZNode;
        updateWorkers();
    }

    @Override
    public void subscribe(Consumer<List<String>> subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public List<String> getWorkers() {
        return new ArrayList<>(workers);
    }

    @Override
    public void process(WatchedEvent event) {
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
        try {
            workers = zooKeeper.getChildren(workersZNode, this);
            log.info("Workers updated: {}", workers);
        } catch (KeeperException | InterruptedException e) {
            log.error("Can't update workers.", e);
            return false;
        }

        return true;
    }

    private void notifySubscribers() {
        for (var sub : subscribers) {
            sub.accept(new ArrayList<>(workers));
        }
        log.info("Subscribers notified about new workers: {}", workers);
    }
}
