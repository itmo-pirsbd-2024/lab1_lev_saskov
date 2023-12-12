package server.pool;

import java.util.List;

import org.springframework.stereotype.Service;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import server.zoo.IWorkersZoo;

@Service
public class WorkersPool implements IWorkersPool {
    private final IWorkersZoo workersZoo;

    public WorkersPool(IWorkersZoo workersZoo) {
        this.workersZoo = workersZoo;
        workersZoo.subscribe(this::workersUpdated);
    }

    @Override
    public void createCollection(String name, int vectorLen) {

    }

    @Override
    public void deleteCollection(String name) {

    }

    @Override
    public void renameCollection(String oldName, String newName) {

    }

    @Override
    public void addToCollection(Embedding embedding, String collectionName) {

    }

    @Override
    public void deleteFromCollection(long id, String collectionName) {

    }

    @Override
    public VectorCollectionResult query(double[] vector, int maxNeighboursCount, String collectionName) {
        return null;
    }

    private void workersUpdated(List<String> updatedWorkers) {

    }
}
