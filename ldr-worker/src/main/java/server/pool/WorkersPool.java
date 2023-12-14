package server.pool;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ldr.client.domen.DistancedEmbedding;
import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import ldr.server.util.FixedSizePriorityQueue;
import server.local.ILocalWorker;
import server.remote.IRemoteWorker;
import server.remote.RemoteWorker;
import server.zoo.ICollectionsSubscriber;
import server.zoo.ICollectionsZoo;
import server.zoo.IWorkersSubscriber;
import server.zoo.IWorkersZoo;

// TODO:
//      Воркеров нельзя поднимать одновременно. Я подписываюсь на изменения только после полной инициализации.
//      Также могут быть проблемы если во время подъема воркера создавать коллекцию.
@Service
public class WorkersPool implements IWorkersPool, IWorkersSubscriber, ICollectionsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(WorkersPool.class);
    private final ICollectionsZoo collectionsZoo;
    private final IWorkersHashing workersHashing;
    private final Map<String, IRemoteWorker> remoteWorkers = new HashMap<>();
    private final ILocalWorker localWorker;

    public WorkersPool(ILocalWorker localWorker, IWorkersZoo workersZoo, ICollectionsZoo collectionsZoo) {
        this.localWorker = localWorker;
        this.collectionsZoo = collectionsZoo;

        List<IWorkersZoo.WorkerInfo> remote = workersZoo.getWorkers();
        workersHashing = new ConsistentHashing(remote.stream().map(IWorkersZoo.WorkerInfo::name).toList());
        for (var w : remote) {
            remoteWorkers.put(w.name(), new RemoteWorker(w.name(), w.host()));
        }

        workersZoo.subscribe(this);
        workersZoo.addWorker(new IWorkersZoo.WorkerInfo(localWorker.getName(), localWorker.getHost()));

        // На изменение коллекций подпишемся, когда придет ответ от zookeeper, что он принял нового воркера,
        // там же и нужные коллекции создадим.
    }

    @Override
    public ResponseEntity<String> createCollection(String name, int vectorLen) {
        collectionsZoo.addCollection(ICollectionsZoo.newCollectionInfo(name, vectorLen));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<String> deleteCollection(String name) {
        collectionsZoo.deleteCollection(name);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> addToCollection(Embedding embedding, String collectionName) {
        IWorkersHashing.Owner owner = workersHashing.getOwner(embedding.id(), collectionName);
        if (owner.worker().equals(localWorker.getName())) {
            localWorker.addToCollection(embedding, owner.internalCollection());
        } else {
            remoteWorkers.get(owner.worker()).addToCollection(embedding, owner.internalCollection());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> addToCollectionInternal(Embedding embedding, String collectionName) {
        localWorker.addToCollection(embedding, collectionName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteFromCollection(long id, String collectionName) {
        IWorkersHashing.Owner owner = workersHashing.getOwner(id, collectionName);
        if (owner.worker().equals(localWorker.getName())) {
            localWorker.deleteFromCollection(id, owner.internalCollection());
        } else {
            remoteWorkers.get(owner.worker()).deleteFromCollection(id, owner.internalCollection());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteFromCollectionInternal(long id, String collectionName) {
        localWorker.deleteFromCollection(id, collectionName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // TODO: Сделать слияние отсортированных массивов. Это будет эффективнее, чем бинарная куча.
    @Override
    public ResponseEntity<VectorCollectionResult> query(double[] vector, int maxNeighboursCount, String collectionName) {
        FixedSizePriorityQueue<DistancedEmbedding> queue = new FixedSizePriorityQueue<>(
                maxNeighboursCount,
                (o1, o2) -> Double.compare(o2.distance(), o1.distance())
        );

        addResultFromWorker(localWorker.query(vector, maxNeighboursCount, collectionName), queue);
        for (IRemoteWorker worker : remoteWorkers.values()) {
            addResultFromWorker(worker.query(vector, maxNeighboursCount, collectionName), queue);
        }

        List<DistancedEmbedding> results = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            results.add(queue.poll());
        }
        Collections.reverse(results);

        return new ResponseEntity<>(new VectorCollectionResult(results), HttpStatus.OK);
    }

    private void addResultFromWorker(ResponseEntity<VectorCollectionResult> entity,
                                     AbstractQueue<DistancedEmbedding> queue) {
        VectorCollectionResult workerResult = entity.getBody();
        if (entity.getStatusCode().equals(HttpStatus.OK) &&
                workerResult != null && !workerResult.isEmpty()) {
            queue.addAll(workerResult.results());
        }
    }

    @Override
    public ResponseEntity<VectorCollectionResult> queryInternal(double[] vector, int maxNeighboursCount, String collectionName) {
        VectorCollectionResult result = localWorker.query(vector, maxNeighboursCount, collectionName).getBody();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public void newWorker(IWorkersZoo.WorkerInfo worker) {
        if (isLocalWorker(worker.name())) {
            Set<Integer> domains = workersHashing.addWorker(worker.name());
            localWorker.extendDomains(domains);

            collectionsZoo.subscribe(this);
            collectionsZoo.getCollections().forEach(this::newCollection);
            log.info("Local worker: {}, host: {} was initialized in worker pool.", worker.name(), worker.host());
        } else {
            IRemoteWorker remoteWorker = new RemoteWorker(worker.name(), worker.host());
            remoteWorkers.put(worker.name(), remoteWorker);
            Set<Integer> domains = workersHashing.addWorker(worker.name());
            localWorker.sendAlign(domains, remoteWorker);
            log.info("Remote worker: {}, host: {} was initialized in worker pool. ", worker.name(), worker.host());
        }
    }

    @Override
    public void deletedWorker(String name) {
        if (localWorker.getName().equals(name)) {
            log.error("FATAL: Local worker lost connection with zookeeper.");
            // TODO: Падать при таком сценарии?
        }
        workersHashing.deleteWorker(name);
        // Один из воркеров выбыл из ротации, надо расширить список доменов для данного воркера.
        boolean extended = localWorker.extendDomains(workersHashing.getDomains(localWorker.getName()));
        if (extended) {
            // Вызываем создание коллекций еще раз, чтобы были созданы для новых доменов.
            // Внутри есть логика, по которой уже созданные коллекции не будут созданы еще раз.
            collectionsZoo.getCollections().forEach(this::newCollection);
        }
    }

    private boolean isLocalWorker(String workerName) {
        return localWorker.getName().equals(workerName);
    }

    @Override
    public void newCollection(ICollectionsZoo.CollectionInfo collection) {
        localWorker.createCollection(collection.name(), collection.vectorLen());
    }

    @Override
    public void deletedCollection(String collection) {
        localWorker.deleteCollection(collection);
    }
}
