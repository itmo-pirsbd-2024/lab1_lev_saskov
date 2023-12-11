package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import client.IWorkerClient;
import ldr.client.domen.DistancedEmbedding;
import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import ldr.server.util.FixedSizePriorityQueue;

public class WorkerPool implements IWorkerPool {
    private final Logger log = LoggerFactory.getLogger(WorkerPool.class);
    private final Map<String, IWorkerClient> workers;
    private final RendezvousHashing rendezvousHashing;

    public WorkerPool(List<IWorkerClient> workers) {
        this.workers = workers.stream().collect(Collectors.toMap(IWorkerClient::getNodeName, Function.identity()));
        this.rendezvousHashing = new RendezvousHashing(
                workers.stream().map(IWorkerClient::getNodeName).toList()
        );
    }

    //TODO: Другой тип возвращаемого значения, говорящий о том, что все создалось.
    @Override
    public void createCollection(String name, int vectorLen) {
        for (IWorkerClient worker : workers.values()) {
            worker.createCollection(name, vectorLen);
        }
    }

    @Override
    public void deleteCollection(String name) {
        for (IWorkerClient worker : workers.values()) {
            worker.deleteCollection(name);
        }
    }

    @Override
    public void renameCollection(String oldName, String newName) {
        for (IWorkerClient worker : workers.values()) {
            worker.renameCollection(oldName, newName);
        }
    }

    @Override
    public void addToCollection(Embedding embedding, String collectionName) {
        getWorkerForId(embedding.id()).addToCollection(embedding, collectionName);
    }

    @Override
    public void deleteFromCollection(long id, String collectionName) {
        getWorkerForId(id).deleteFromCollection(id, collectionName);
    }

    private IWorkerClient getWorkerForId(long id) {
        String workerName = rendezvousHashing.getNodeForId(id);
        return workers.get(workerName);
    }

    // TODO: Сделать слияние отсортированных массивов. Это будет эффективнее, чем бинарная куча.
    @Override
    public VectorCollectionResult query(double[] vector, int maxNeighboursCount, String collectionName) {
        FixedSizePriorityQueue<DistancedEmbedding> queue = new FixedSizePriorityQueue<>(
                maxNeighboursCount,
                (o1, o2) -> Double.compare(o2.distance(), o1.distance())
        );
        for (IWorkerClient worker : workers.values()) {
            var workerEntity = worker.query(vector, maxNeighboursCount, collectionName);
            VectorCollectionResult workerResult = workerEntity.getBody();
            if (workerEntity.getStatusCode().equals(HttpStatus.OK) &&
                    workerResult != null && !workerResult.isEmpty()) {
                queue.addAll(workerResult.results());
            }
        }

        List<DistancedEmbedding> results = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            results.add(queue.poll());
        }
        Collections.reverse(results);
        return new VectorCollectionResult(results);
    }
}
