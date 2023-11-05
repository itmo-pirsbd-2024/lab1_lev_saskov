package ldr.server.serialization.my;

import java.nio.ByteBuffer;

/**
 * VarInt is the same as varLong, so we reuse it.
 */
public class VarIntEncoder implements DataEncoder<Integer> {
    private static final DataEncoder<Long> longEncoder = new VarLongEncoder();

    @Override
    public DecodeResult<Integer> decode(byte[] bytes, int from) {
        return withCheck(longEncoder.decode(bytes, from));
    }

    @Override
    public DecodeResult<Integer> decode(ByteBuffer byteBuffer, int from) {
        return withCheck(longEncoder.decode(byteBuffer, from));
    }

    @Override
    public byte[] encode(Integer data) {
        return longEncoder.encode((long) data);
    }

    private static DecodeResult<Integer> withCheck(DecodeResult<Long> longRes) {
        if (longRes.bytesCount() > Integer.BYTES + 1) {
            throw new RuntimeException("Too many bytes in VarInt");
        }
        return new DecodeResult<>((int) (long) longRes.result(), longRes.bytesCount());
    }
}
