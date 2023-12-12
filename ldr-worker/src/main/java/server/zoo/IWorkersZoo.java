package server.zoo;

import java.util.List;
import java.util.function.Consumer;

public interface IWorkersZoo {
    /**
     * Subscribe on worker pool changes.
     *
     * @param subscriber - after each worker pool update will be called with new list of workers.
     *                   unmodifiable list will be accepted.
     */
    void subscribe(Consumer<List<String>> subscriber);

    /**
     * @return unmodifiable list of last updated workers.
     */
    List<String> getWorkers();
}
