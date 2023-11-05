package ldr.server.serialization.my;

import org.junit.jupiter.api.Test;

import ldr.client.domen.Embedding;
import ldr.server.serialization.Embeddings;

class EmbeddingEncoderTest {
    @Test
    public void testCoding() {
        DataEncoder<Embedding> coder = new EmbeddingEncoder();
        CoderTestUtil.testCoding(Embeddings.examples, coder);
    }
}