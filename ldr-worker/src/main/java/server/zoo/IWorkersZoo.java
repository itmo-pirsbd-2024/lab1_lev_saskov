package server.zoo;

import java.util.List;

public interface IWorkersZoo {
    /**
     * Subscribe on worker pool changes.
     *
     * @param subscriber - after each worker pool update will be called with new list of workers.
     *                   unmodifiable list will be accepted.
     */
    void subscribe(IWorkersSubscriber subscriber);

    /**
     * @return list of last updated workers.
     */
    List<WorkerInfo> getWorkers();

    void addWorker(WorkerInfo worker);

    record WorkerInfo(String name, String host) {}
}
