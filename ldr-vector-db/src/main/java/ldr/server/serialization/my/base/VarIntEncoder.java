package ldr.server.serialization.my.base;

import java.nio.ByteBuffer;
import java.util.List;

public class VarIntEncoder {
    private static final VarLongEncoder longEncoder = new VarLongEncoder();

    public byte[] encode(int data) {
        return longEncoder.encode(data);
    }

    public IntDecodeResult decode(byte[] bytes, int from) {
        return withCheck(longEncoder.decode(bytes, from));
    }

    public IntDecodeResult decode(ByteBuffer byteBuffer, int from) {
        return withCheck(longEncoder.decode(byteBuffer, from));
    }

    public IntDecodeResult decode(byte[] bytes) {
        return decode(bytes, 0);
    }

    public IntDecodeResult decode(ByteBuffer byteBuffer) {
        return decode(byteBuffer, 0);
    }

    public int putToList(List<byte[]> bytesList, int toEncode) {
        byte[] result = encode(toEncode);
        bytesList.add(result);
        return result.length;
    }

    private static IntDecodeResult withCheck(VarLongEncoder.LongDecodeResult longRes) {
        if (longRes.bytesCount() > Integer.BYTES + 1) {
            throw new RuntimeException("Too many bytes in VarInt");
        }
        return new IntDecodeResult((int) longRes.result(), longRes.bytesCount());
    }

    public record IntDecodeResult(int result, int bytesCount) {
    }
}