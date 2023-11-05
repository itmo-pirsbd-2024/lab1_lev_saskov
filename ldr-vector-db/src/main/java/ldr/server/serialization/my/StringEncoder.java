package ldr.server.serialization.my;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.ArrayUtils;

public class StringEncoder implements DataEncoder<String> {
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final DataEncoder<Integer> intCoder = new VarIntEncoder();

    @Override
    public DecodeResult<String> decode(byte[] bytes, int from) {
        DecodeResult<Integer> strSizeDecode = intCoder.decode(bytes, from);
        String str = new String(bytes, from + strSizeDecode.bytesCount(), strSizeDecode.result(), charset);

        return new DecodeResult<>(str, strSizeDecode.bytesCount() + strSizeDecode.result());
    }

    @Override
    public DecodeResult<String> decode(ByteBuffer byteBuffer, int from) {
        DecodeResult<Integer> strSizeDecode = intCoder.decode(byteBuffer, from);
        byte[] strBytes = new byte[strSizeDecode.result()];
        byteBuffer.get(from + strSizeDecode.bytesCount(), strBytes);
        String str = new String(strBytes, charset);

        return new DecodeResult<>(str, strSizeDecode.bytesCount() + strSizeDecode.result());
    }

    @Override
    public byte[] encode(String data) {
        byte[] codedStr = data.getBytes(StandardCharsets.UTF_8);
        byte[] codedSize = intCoder.encode(codedStr.length);

        return ArrayUtils.addAll(codedSize, codedStr);
    }
}
