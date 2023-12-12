package server.pool;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentHashingTest {
    private static final Logger log = LoggerFactory.getLogger(ConsistentHashingTest.class);

    @Test
    public void testAddWorkers() {
        List<String> workers = new ArrayList<>(){{add("worker1"); add("worker2"); add("worker3");}};
        IWorkersHashing hashing = new ConsistentHashing(workers);

        // Just check that added worker have some domains.
        String newWorker = "worker4";
        assertTrue(hashing.addWorker(newWorker).size() > 10);
        workers.add(newWorker);
        for (long id = -5_000; id < 5_000; id++) {
            IWorkersHashing.Owner owner = hashing.getOwner(id, "coll");
            assertTrue(owner.internalCollection().startsWith("coll"));
            assertTrue(workers.contains(owner.worker()));
//            log.info("For id {}, found worker: {}, collection: {}", id, owner.worker(), owner.internalCollection());
        }
    }

}