package server.local;

import java.util.Set;

import org.springframework.http.ResponseEntity;

import server.remote.IRemoteWorker;
import server.remote.IWorker;

public interface ILocalWorker extends IWorker {
    ResponseEntity<String> createCollection(String name, int vectorLen);
    ResponseEntity<String> deleteCollection(String name);

    /**
     * Данная операция выдаст ошибку, если множество переданных доменов не будет включать в себя множество старых доменов,
     * поскольку для сужения множества доменов нужна пересылка, для таких случаев используй sendAlign.
     */
    boolean extendDomains(Set<Integer> newDomains);

    /**
     * Воркер вычитает из собственных доменов alignDomains и отдает чужие домены новому владельцу.
     * Воркер должен переслать все коллекции, которые ему больше не принадлежат новому владельцу.
     */
    void sendAlign(Set<Integer> alignDomains, IRemoteWorker owner);
}
