package server.zoo;

/**
 * Subscriber for collections zoo changes.
 */
public interface ICollectionsSubscriber {
    void newCollection(ICollectionsZoo.CollectionInfo collection);
    void deletedCollection(String collectionName);
}
