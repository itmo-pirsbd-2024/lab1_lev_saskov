package ldr.server.serialization.my;

import java.util.List;

import static ldr.server.serialization.my.DataEncoder.DecodeResult;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoderTestUtil {
    public static <D> void testCoding(List<D> expList, DataEncoder<D> coder) {
        for (D x : expList) {
            byte[] encodedBytes = coder.encode(x);
            DecodeResult<D> decoded = coder.decode(encodedBytes);
            assertEquals(encodedBytes.length, decoded.bytesCount());
            assertEquals(x, decoded.result());
        }
    }
}
