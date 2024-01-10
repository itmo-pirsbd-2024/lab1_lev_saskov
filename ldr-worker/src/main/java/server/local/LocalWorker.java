package server.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import ldr.client.domen.DistancedEmbedding;
import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import ldr.client.domen.db.IDataBase;
import ldr.server.util.FixedSizePriorityQueue;
import server.naming.Utils;
import server.remote.IRemoteWorker;
import server.remote.Worker;

@Component
public class LocalWorker extends Worker implements ILocalWorker {
    private static final Logger log = LoggerFactory.getLogger(LocalWorker.class);

    private final IDataBase dataBase;
    private Set<Integer> domains;

    public LocalWorker(@Value("${local.worker.name}") String name, @Value("${server.port}") String host,
                       IDataBase dataBase) {
        // TODO: For localhost it is ok, but for other will be bad
        super(name, "127.0.0.1:" + host);
        this.dataBase = dataBase;
    }

    @Override
    public ResponseEntity<String> createCollection(String name, int vectorLen) {
        List<String> localCollections = dataBase.getAllCollections();
        for (int domain : domains) {
            try {
                String collInternal = Utils.getInternalCollection(name, domain);
                // Проверка потому что при поднятии воркера я создаю все актуальные коллекции из зукипера опять.
                if (!localCollections.contains(collInternal)) {
                    dataBase.createCollection(Utils.getInternalCollection(name, domain), vectorLen);
                }
            } catch (IOException e) {
                return new ResponseEntity<>("Can't create collection " + name + " " + e,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Collection " + name + "created.", HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<String> deleteCollection(String name) {
        for (int domain : domains) {
            try {
                dataBase.removeCollection(name + domain);
            } catch (IOException e) {
                return new ResponseEntity<>("Can't remove collection " + name + " " + e,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Collection " + name + "removed.", HttpStatus.OK);
    }

    /**
     * @param collectionName - имя коллекции приходит сразу с доменом. Домен приклеивается в WorkersPool.
     */
    @Override
    public ResponseEntity<String> addToCollection(Embedding embedding, String collectionName) {
        dataBase.getCollection(collectionName).add(embedding);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * @param collectionName - имя коллекции приходит сразу с доменом. Домен приклеивается в WorkersPool.
     */
    @Override
    public ResponseEntity<String> deleteFromCollection(long id, String collectionName) {
        dataBase.getCollection(collectionName).delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * @param collectionName - имя коллекции приходит без домена.
     *                       Берутся все домены по этому воркеру и делается reduce.
     */
    @Override
    public ResponseEntity<VectorCollectionResult> query(double[] vector, int maxNeighboursCount, String collectionName) {
        FixedSizePriorityQueue<DistancedEmbedding> queue = new FixedSizePriorityQueue<>(
                maxNeighboursCount,
                (o1, o2) -> Double.compare(o2.distance(), o1.distance())
        );

        for (int domain : domains) {
            String domainColl = Utils.getInternalCollection(collectionName, domain);
            var res = dataBase.getCollection(domainColl).query(vector, maxNeighboursCount);
            if (res != null && !res.isEmpty()) {
                queue.addAll(res.results());
            }
        }

        List<DistancedEmbedding> results = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            results.add(queue.poll());
        }
        Collections.reverse(results);

        return new ResponseEntity<>(new VectorCollectionResult(results), HttpStatus.OK);
    }

    @Override
    public boolean extendDomains(Set<Integer> newDomains) {
        if (this.domains != null && !newDomains.containsAll(this.domains)) {
            log.error("Set domains should be used only for domain extension. Old domains: {}, new domains: {}",
                    this.domains, newDomains);
            return false;
        }

        boolean extended = domains == null || newDomains.size() > domains.size();
        this.domains = newDomains;
        return extended;
    }

    @Override
    public void sendAlign(Set<Integer> alignDomains, IRemoteWorker owner) {
        Set<Integer> domainsToSend = new HashSet<>(domains);
        domainsToSend.retainAll(alignDomains);
        if (domainsToSend.isEmpty()) {
            return;
        }

        // На удаленном воркере уже должны быть эти коллекции, он их достал из зукипера и создал, когда поднимался.
        domains.removeAll(domainsToSend);
        List<String> toSendCollections = getCollectionsToSend(dataBase.getAllCollections(), alignDomains);
        for (String toSendColl : toSendCollections) {
            var ans = owner.sendCollection(toSendColl, dataBase.getCollection(toSendColl));
            if (ans.getStatusCode() == HttpStatus.OK) {
                log.info("Collection {} was sent to new owner", toSendColl);
                try {
                    dataBase.removeCollection(toSendColl);
                } catch (IOException e) {
                    log.error("Error during collection {} remove after sending to other worker.", toSendColl, e);
                }
            } else {
                log.error("Error during collection {} sending to other worker.", toSendColl);
            }
        }
    }

    @VisibleForTesting
    static List<String> getCollectionsToSend(List<String> collections, Set<Integer> domainsToSend) {
        String regex = Utils.getDomainCollectionRegex(domainsToSend);

        return collections.stream().filter(c -> c.matches(regex)).toList();
    }
}
