package ldr.server.serialization.my.base;

import java.nio.ByteBuffer;
import java.util.List;

public class DoubleEncoder {

    public byte[] encode(double data) {
        return ByteBuffer.allocate(Double.BYTES).putDouble(data).array();
    }

    public DoubleDecodeResult decode(byte[] bytes, int from) {
        return decode(ByteBuffer.wrap(bytes), from);
    }

    public DoubleDecodeResult decode(ByteBuffer byteBuffer, int from) {
        return new DoubleDecodeResult(byteBuffer.getDouble(from), Double.BYTES);
    }

    public int putToList(List<byte[]> bytesList, double toEncode) {
        byte[] result = encode(toEncode);
        bytesList.add(result);
        return result.length;
    }

    public record DoubleDecodeResult(double result, int bytesCount) {
    }
}
