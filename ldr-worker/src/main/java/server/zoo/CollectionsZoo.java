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
import ldr.server.serialization.my.VarIntEncoder;

import static server.zoo.ZooUtils.getDelta;

// TODO: Можно сделать абстрактный класс с дженериками Zoo и вынести туда всю логику.
//  Сделать 2 наследника CollectionsZoo и WorkersZoo
@Service
public class CollectionsZoo implements ICollectionsZoo, Watcher {
    private static final Logger log = LoggerFactory.getLogger(CollectionsZoo.class);

    private final DataEncoder<Integer> vectorLenCoder = new VarIntEncoder();
    private final List<ICollectionsSubscriber> subscribers = new ArrayList<>();
    // Init to avoid NPE during first collections update.
    private Set<String> prevCollections = new HashSet<>();
    private Set<String> collections = new HashSet<>();

    private final ZooKeeper zooKeeper;
    private final String collectionsZNode;

    public CollectionsZoo(ZooKeeper zooKeeper, @Value("${collections.zoo.znode}") String collectionsZNode) {
        this.zooKeeper = zooKeeper;
        this.collectionsZNode = collectionsZNode;
        updateCollections();
    }

    @Override
    public void subscribe(ICollectionsSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public List<CollectionInfo> getCollections() {
        List<CollectionInfo> result = new ArrayList<>(collections.size());
        for (String name : collections) {
            try {
                result.add(getCollection(name));
            } catch (InterruptedException | KeeperException e) {
                log.error("Can't resolve collection during getCollections: {}", name, e);
            }
        }

        return result;

    }

    @Override
    public void addCollection(CollectionInfo collection) {
        try {
            zooKeeper.create(collectionsZNode + "/" + collection.name(),
                    vectorLenCoder.encode(collection.vectorLen()),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            log.error("Can't create collection {}.", collection, e);
        }
    }

    @Override
    public void deleteCollection(String name) {
        try {
            zooKeeper.delete(collectionsZNode + "/" + name, -1);
        } catch (InterruptedException | KeeperException e) {
            log.error("Can't delete collection {}.", name, e);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getPath().equals(collectionsZNode) &&
                event.getType() == Event.EventType.NodeChildrenChanged) {
            if (updateCollections()) {
                notifySubscribers();
            }
        } else {
            log.error("Unsupported event: {}", event);
        }
    }

    private void notifySubscribers() {
        if (collections.size() > prevCollections.size()) {
            String newCollection = getDelta(collections, prevCollections);
            CollectionInfo collectionInfo;
            try {
                collectionInfo = getCollection(newCollection);
            } catch (InterruptedException | KeeperException e) {
                log.error("Can't get collection {} during notify subscribers.", newCollection, e);
                return;
            }

            for (var sub : subscribers) {
                // TODO: Get data with host from this znode
                sub.newCollection(collectionInfo);
            }

            log.info("Subscribers notified about new collection: {}", collectionInfo);
        } else if (collections.size() < prevCollections.size()) {
            String deletedCollection = getDelta(prevCollections, collections);
            for (var sub : subscribers) {
                sub.deletedCollection(deletedCollection);
            }

            log.info("Subscribers notified about deleted collection: {}", deletedCollection);
        } else {
            log.error("Attempt to notify subscribers but collections didn't change. Collections: {}. Previous collections: {}",
                    collections, prevCollections);
        }
    }

    private CollectionInfo getCollection(String collectionName) throws InterruptedException, KeeperException {
        // TODO: Check that path is correct
        byte[] hostBytes = zooKeeper.getData(collectionsZNode + "/" + collectionName, false, null);
        return new CollectionInfo(collectionName, vectorLenCoder.decode(hostBytes).result());
    }

    private boolean updateCollections() {
        prevCollections = collections;
        try {
            collections = new HashSet<>(zooKeeper.getChildren(collectionsZNode, this));
            log.info("Collections updated: {}", collections);
        } catch (KeeperException | InterruptedException e) {
            log.error("Can't update collections.", e);
            return false;
        }

        return true;
    }
}
