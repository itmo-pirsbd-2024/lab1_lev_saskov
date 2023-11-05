package ldr.server.serialization.my;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class StringMapEncoder extends AbstractDataEncoder<Map<String, String>> {
    private static final DataEncoder<Integer> intCoder = new VarIntEncoder();
    private static final DataEncoder<String> strCoder = new StringEncoder();

    @Override
    public byte[] encode(Map<String, String> data) {
        List<byte[]> mapBytesList = new ArrayList<>(data.size() * 2 + 1);
        int sumBytesCount = 0;
        sumBytesCount += putToList(mapBytesList, data.size(), intCoder);
        for (var entry : data.entrySet()) {
            sumBytesCount += putToList(mapBytesList, entry.getKey(), strCoder);
            sumBytesCount += putToList(mapBytesList, entry.getValue(), strCoder);
        }

        byte[] result = new byte[sumBytesCount];
        unwrapList(result, mapBytesList);
        return result;
    }

    @Override
    public DecodeResult<Map<String, String>> decode(byte[] bytes, int from) {
        return decode(from, intCoder.decode(bytes, from), offset -> strCoder.decode(bytes, offset));
    }

    @Override
    public DecodeResult<Map<String, String>> decode(ByteBuffer byteBuffer, int from) {
        return decode(from, intCoder.decode(byteBuffer, from), offset -> strCoder.decode(byteBuffer, offset));
    }

    private DecodeResult<Map<String, String>> decode(int from, DecodeResult<Integer> sizeDecode,
                                                     Function<Integer, DecodeResult<String>> strGetter) {
        Map<String, String> result = new HashMap<>(sizeDecode.result());
        int offset = from + sizeDecode.bytesCount();
        for (int i = 0; i < sizeDecode.result(); i++) {
            DecodeResult<String> keyDec = strGetter.apply(offset);
            offset += keyDec.bytesCount();
            DecodeResult<String> valDec = strGetter.apply(offset);
            offset += valDec.bytesCount();

            result.put(keyDec.result(), valDec.result());
        }

        return new DecodeResult<>(result, offset - from);
    }
}
