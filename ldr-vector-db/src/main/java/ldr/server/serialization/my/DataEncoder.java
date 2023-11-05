package ldr.server.serialization.my;

import java.nio.ByteBuffer;

public interface DataEncoder<D> {
    byte[] encode(D data);

    DecodeResult<D> decode(byte[] bytes, int from);

    DecodeResult<D> decode(ByteBuffer byteBuffer, int from);

    default DecodeResult<D> decode(byte[] bytes) {
        return decode(bytes, 0);
    }

    default DecodeResult<D> decode(ByteBuffer byteBuffer) {
        return decode(byteBuffer, 0);
    }

    /**
     * @param result     of decoding.
     * @param bytesCount - count of read bytes during decoding (size of decoded info in coded byte view).
     */
    record DecodeResult<T>(T result, int bytesCount) {
    }
}
