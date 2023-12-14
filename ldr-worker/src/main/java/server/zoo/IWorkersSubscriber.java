package server.zoo;

/**
 * Subscriber for forkers zoo changes.
 */
public interface IWorkersSubscriber {
    void newWorker(IWorkersZoo.WorkerInfo worker);
    void deletedWorker(String name);
}
