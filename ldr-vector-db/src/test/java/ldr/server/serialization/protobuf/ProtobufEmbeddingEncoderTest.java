package ldr.server.serialization.protobuf;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ldr.client.domen.Embedding;
import ldr.server.serialization.Embeddings;
import ldr.server.serialization.my.CoderTestUtil;
import ldr.server.serialization.my.DataEncoder;

class ProtobufEmbeddingEncoderTest {
    @Disabled("Protobuf encoding is deprecated.")
    @Test
    public void testCoding() {
        DataEncoder<Embedding> coder = new ProtobufEmbeddingEncoder();
        CoderTestUtil.testCoding(Embeddings.examples, coder);
    }
}