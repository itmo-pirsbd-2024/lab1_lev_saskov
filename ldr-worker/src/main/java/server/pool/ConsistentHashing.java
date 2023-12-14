package server.pool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.jshell.execution.Util;
import server.naming.Utils;

public class ConsistentHashing implements IWorkersHashing {
    private static final Logger log = LoggerFactory.getLogger(ConsistentHashing.class);

    private static final int DOMAINS_COUNT = 256;
    private static final int V_NODES_FACTOR = 3;
    private static final String HASH_SUFFIX = "abcdefghijklmnoprstuvwxyz";

    private final TreeMap<Integer, String> circle;

    public ConsistentHashing(List<String> workers) {
        this.circle = new TreeMap<>();
        for (String worker : workers) {
            addWorker(worker);
        }
    }

    @Override
    public Set<Integer> addWorker(String name) {
        int[] borderDomains = getWorkerBorderDomains(name);
        for (int domain : borderDomains) {
            circle.put(domain, name);
        }
        log.info("Circle border domains for new worker: {}", Arrays.toString(borderDomains));

        Set<Integer> result = getDomains(name);
        log.info("Added new worker: {}. It responsible for domains: {}", name, result);
        return result;
    }

    // TODO: worker выбывший из ротации и потом вошедший с непочищенными данными все сломает?
    @Override
    public void deleteWorker(String name) {
        int[] borderDomains = getWorkerBorderDomains(name);
        for (int domain : borderDomains) {
            String responsibleWorker = circle.get(domain);
            if (responsibleWorker.equals(name)) {
                circle.remove(domain);
            } else {
                log.error("Error deleting worker circle another worker responsible for this domain, " +
                        "domain: {}, deleting worker: {}, responsible worker: {}", domain, name, responsibleWorker);
            }
        }
        log.info("Circle border domains for new worker: {}", Arrays.toString(borderDomains));

    }

    private int[] getWorkerBorderDomains(String worker) {
        int[] result = new int[V_NODES_FACTOR];

        // TODO: This type of hashing with strings can be bad for real systems, use murmur or smth cool.
        StringBuilder virtualNodeHashStr = new StringBuilder();
        for (int i = 0; i < V_NODES_FACTOR; i++) {
            virtualNodeHashStr.append(worker).append(HASH_SUFFIX);
            String virtualNode = virtualNodeHashStr.append(i).toString();
            result[i] = getDomain(virtualNode.hashCode());
        }

        return result;
    }

    @Override
    public Owner getOwner(long id, String collection) {
        int domain = getDomain(id);

        String worker = getWorker(domain);
        String internalCollection = Utils.getInternalCollection(collection, domain);

        return new Owner(worker, internalCollection);
    }

    @Override
    public Set<Integer> getDomains(String workerName) {
        Set<Integer> result = new HashSet<>();
        // TODO: Не оптимально, наверное, можно проще?
        for (int domain = 0; domain < DOMAINS_COUNT; domain++) {
            if (getWorker(domain).equals(workerName)) {
                result.add(domain);
            }
        }

        return result;
    }

    private String getWorker(int domain) {
        // ближайший домен в кольце (воркер этого домена отвечает и за предыдущие)
        Integer nextDomain = circle.ceilingKey(domain);
        if (nextDomain == null) {
            // выбираем первый в кольце
            nextDomain = circle.firstKey();
        }

        return circle.get(nextDomain);
    }

    private int getDomain(long num) {
        // To avoid hash below zero.
        return Math.abs((int) num % DOMAINS_COUNT);
    }
}
