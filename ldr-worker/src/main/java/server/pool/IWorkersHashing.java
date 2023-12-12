package server.pool;

import java.util.List;

public interface IWorkersHashing {
    /**
     * Возвращает кортеж виртуальных доменов, которые принадлежат новому воркеру
     */
    List<Integer> addWorker(String worker);
    void deleteWorker(String worker);

    /**
     * @param id - id of vector.
     * @param collection - external, viewed by client collection.
     */
    Owner getOwner(long id, String collection);

    /**
     *
     * @param worker - name of worker, usually host.
     * @param internalCollection - we add suffix to collection. suffix is Domain. We have 256 domains.
     *                           It is actual name of collection for this id in this worker.
     */
    record Owner(String worker, String internalCollection){}
}
