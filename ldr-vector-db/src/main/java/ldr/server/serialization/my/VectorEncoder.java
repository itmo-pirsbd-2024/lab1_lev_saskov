package ldr.server.serialization.my;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VectorEncoder extends AbstractDataEncoder<double[]> {
    DataEncoder<Integer> intCoder = new VarIntEncoder();
    DataEncoder<Double> doubleEncoder = new DoubleEncoder();

    @Override
    public byte[] encode(double[] data) {
        List<byte[]> bytesList = new ArrayList<>(data.length + 1);
        int sumBytesCount = 0;
        sumBytesCount += putToList(bytesList, data.length, intCoder);
        for (double el : data) {
            sumBytesCount += putToList(bytesList, el, doubleEncoder);
        }

        byte[] result = new byte[sumBytesCount];
        unwrapList(result, bytesList);

        return result;
    }

    @Override
    public DecodeResult<double[]> decode(byte[] bytes, int from) {
        return decode(from, intCoder.decode(bytes, from),  offset -> doubleEncoder.decode(bytes, offset));
    }

    @Override
    public DecodeResult<double[]> decode(ByteBuffer byteBuffer, int from) {
        return decode(from, intCoder.decode(byteBuffer, from),  offset -> doubleEncoder.decode(byteBuffer, offset));
    }

    private DecodeResult<double[]> decode(int from, DecodeResult<Integer> sizeDecode,
                                          Function<Integer, DecodeResult<Double>> doubleGetter) {
        double[] result = new double[sizeDecode.result()];
        int offset = from + sizeDecode.bytesCount();
        for (int i = 0; i < sizeDecode.result(); i++) {
            DecodeResult<Double> valDec = doubleGetter.apply(offset);
            offset += valDec.bytesCount();
            result[i] = valDec.result();
        }

        return new DecodeResult<>(result, offset - from);

    }
}
