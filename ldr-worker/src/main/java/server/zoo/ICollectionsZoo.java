package server.zoo;

import java.util.List;

public interface ICollectionsZoo {
    /**
     * Subscribe on collections changes.
     */
    void subscribe(ICollectionsSubscriber subscriber);

    /**
     * @return list of last updated collections.
     */
    List<CollectionInfo> getCollections();

    void addCollection(CollectionInfo collection);
    void deleteCollection(String name);

    static CollectionInfo newCollectionInfo(String name, int vectorLen) {
        return new CollectionInfo(name, vectorLen);
    }

    record CollectionInfo(String name, int vectorLen){}
}
