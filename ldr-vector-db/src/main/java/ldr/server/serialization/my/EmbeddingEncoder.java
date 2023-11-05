package ldr.server.serialization.my;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ldr.client.domen.Embedding;

public class EmbeddingEncoder extends AbstractDataEncoder<Embedding> {
    private static final DataEncoder<Long> longCoder = new VarLongEncoder();
    private static final DataEncoder<double[]> vectorCoder = new VectorEncoder();
    private static final DataEncoder<Map<String, String>> metaCoder = new StringMapEncoder();

    @Override
    public byte[] encode(Embedding data) {
        List<byte[]> bytesList = new ArrayList<>(1 + data.vector().length + data.metas().size() * 2);
        int sumBytesCount = 0;
        sumBytesCount += putToList(bytesList, data.id(), longCoder);
        sumBytesCount += putToList(bytesList, data.vector(), vectorCoder);
        sumBytesCount += putToList(bytesList, data.metas(), metaCoder);

        byte[] result = new byte[sumBytesCount];
        unwrapList(result, bytesList);
        return result;
    }

    @Override
    public DecodeResult<Embedding> decode(byte[] bytes, int from) {
        int offset = from;
        var idDec = longCoder.decode(bytes, offset);
        offset += idDec.bytesCount();
        var vecDec = vectorCoder.decode(bytes, offset);
        offset += vecDec.bytesCount();
        var metaDec = metaCoder.decode(bytes, offset);
        offset += metaDec.bytesCount();

        return new DecodeResult<>(new Embedding(idDec.result(), vecDec.result(), metaDec.result()), offset);
    }

    @Override
    public DecodeResult<Embedding> decode(ByteBuffer byteBuffer, int from) {
        int offset = from;
        var idDec = longCoder.decode(byteBuffer, offset);
        offset += idDec.bytesCount();
        var vecDec = vectorCoder.decode(byteBuffer, offset);
        offset += vecDec.bytesCount();
        var metaDec = metaCoder.decode(byteBuffer, offset);
        offset += metaDec.bytesCount();

        return new DecodeResult<>(new Embedding(idDec.result(), vecDec.result(), metaDec.result()), offset);
    }
}
