package ldr.server.serialization.protobuf;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.lang3.NotImplementedException;

import com.google.protobuf.InvalidProtocolBufferException;
import ldr.client.domen.Embedding;
import ldr.server.serialization.my.DataEncoder;

// Use only for Lab1 with Koroleva (comparing with my EmbeddingEncoder).
// This encoder don't support bytesCount in DataEncoder.
@Deprecated
public class ProtobufEmbeddingEncoder implements DataEncoder<Embedding> {
    @Override
    public DecodeResult<Embedding> decode(byte[] bytes) {
        EmbeddingOuterClass.Embedding parsedEmbedding;
        try {
            parsedEmbedding = EmbeddingOuterClass.Embedding.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

        return new DecodeResult<>(
                new Embedding(
                        parsedEmbedding.getId(),
                        parsedEmbedding.getValueList().stream().mapToDouble(Double::doubleValue).toArray(),
                        parsedEmbedding.getMetasMap()
                ),
                bytes.length
        );
    }

    @Override
    public DecodeResult<Embedding> decode(byte[] bytes, int from) {
        throw new NotImplementedException();
    }

    @Override
    public DecodeResult<Embedding> decode(ByteBuffer byteBuffer, int from) {
        throw new NotImplementedException();
    }

    @Override
    public byte[] encode(Embedding embedding) {
        return EmbeddingOuterClass.Embedding.newBuilder()
                .setId(embedding.id())
                .addAllValue(Arrays.stream(embedding.vector()).boxed().toList())
                .putAllMetas(embedding.metas())
                .build()
                .toByteArray();
    }
}
