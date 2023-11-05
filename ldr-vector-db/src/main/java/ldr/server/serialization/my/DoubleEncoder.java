package ldr.server.serialization.my;

import java.nio.ByteBuffer;

public class DoubleEncoder implements DataEncoder<Double> {
    @Override
    public byte[] encode(Double data) {
        return ByteBuffer.allocate(Double.BYTES).putDouble(data).array();
    }

    @Override
    public DecodeResult<Double> decode(byte[] bytes, int from) {
        return decode(ByteBuffer.wrap(bytes), from);
    }

    @Override
    public DecodeResult<Double> decode(ByteBuffer byteBuffer, int from) {
        return new DecodeResult<>(byteBuffer.getDouble(from), Double.BYTES);
    }
}
