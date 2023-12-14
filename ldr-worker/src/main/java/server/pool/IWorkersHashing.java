package server.pool;

import java.util.Set;

public interface IWorkersHashing {
    /**
     * Возвращает кортеж виртуальных доменов, которые принадлежат новому воркеру
     */
    Set<Integer> addWorker(String name);
    void deleteWorker(String name);

    /**
     * @param id - id of vector.
     * @param collection - external, viewed by client collection.
     */
    Owner getOwner(long id, String collection);

    /**
     * Get all domains for worker.
     */
    Set<Integer> getDomains(String workerName);

    /**
     *
     * @param worker - name of worker.
     * @param internalCollection - we add suffix to collection. suffix is Domain. We have 256 domains.
     *                           It is actual name of collection for this id in this worker.
     */
    record Owner(String worker, String internalCollection){}
}
