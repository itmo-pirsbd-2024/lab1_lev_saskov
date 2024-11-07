package ldr.server.serialization.my;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import ldr.server.serialization.my.base.DoubleEncoder;
import ldr.server.serialization.my.base.VarIntEncoder;

public class VectorEncoder extends AbstractDataEncoder<double[]> {
    private static final VarIntEncoder intCoder = new VarIntEncoder();
    private static final DoubleEncoder doubleEncoder = new DoubleEncoder();

    @Override
    public byte[] encode(double[] data) {
        List<byte[]> bytesList = new ArrayList<>(data.length + 1);
        int sumBytesCount = 0;
        sumBytesCount += intCoder.putToList(bytesList, data.length);
        for (double el : data) {
            sumBytesCount += doubleEncoder.putToList(bytesList, el);
        }

        byte[] result = new byte[sumBytesCount];
        unwrapList(result, bytesList);

        return result;
    }

    @Override
    public DecodeResult<double[]> decode(byte[] bytes, int from) {
        return decode(from, intCoder.decode(bytes, from), offset -> doubleEncoder.decode(bytes, offset));
    }

    @Override
    public DecodeResult<double[]> decode(ByteBuffer byteBuffer, int from) {
        return decode(from, intCoder.decode(byteBuffer, from), offset -> doubleEncoder.decode(byteBuffer, offset));
    }

    private DecodeResult<double[]> decode(
        int from, VarIntEncoder.IntDecodeResult sizeDecode,
        DoubleGetter doubleGetter
    ) {
        double[] result = new double[sizeDecode.result()];
        int offset = from + sizeDecode.bytesCount();
        for (int i = 0; i < sizeDecode.result(); i++) {
            var valDec = doubleGetter.get(offset);
            offset += valDec.bytesCount();
            result[i] = valDec.result();
        }

        return new DecodeResult<>(result, offset - from);
    }

    @FunctionalInterface
    interface DoubleGetter {
        DoubleEncoder.DoubleDecodeResult get(int offset);
    }
}
