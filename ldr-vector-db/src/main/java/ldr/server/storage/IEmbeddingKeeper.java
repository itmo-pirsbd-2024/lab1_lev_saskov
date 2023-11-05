package ldr.server.storage;

import java.util.List;

import ldr.client.domen.Embedding;

/**
 * Умеет хранить Embedding-и.
 * Не умеет удалять, за это отвечает IStorageManager, поскольку процесс удаление нетривиален,
 * используются могилы, как в LSM.
 */
public interface IEmbeddingKeeper {
    void add(Embedding embedding);
    void add(List<Embedding> embeddings);
}
