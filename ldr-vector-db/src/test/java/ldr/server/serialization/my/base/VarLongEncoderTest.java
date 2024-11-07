package ldr.server.serialization.my.base;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VarLongEncoderTest {
    @Test
    public void testCoding() {
        List<Long> expList = List.of(Long.MAX_VALUE / 2, Long.MIN_VALUE / 2, -1231231L, 123L, 0L);
        var coder = new VarLongEncoder();

        testCoding(expList, coder);
    }

    @Test
    public void testOutOfBorders() {
        List<Long> unsupported = List.of(Long.MAX_VALUE, Long.MAX_VALUE - 500, Long.MIN_VALUE, Long.MIN_VALUE + 500);
        var coder = new VarLongEncoder();

        for (long x : unsupported) {
            assertThrows(RuntimeException.class, () -> coder.encode(x));
        }
    }

    @Test
    public void testTooManyBytes() {
        // Because one bit of all bytes is continued flag, the size can be Long.BYTES + 1 max.
        byte[] bytes = new byte[Long.BYTES + 2];
        Arrays.fill(bytes, (byte) VarLongEncoder.CONTINUE_BIT);
        var coder = new VarLongEncoder();

        assertThrows(RuntimeException.class, () -> coder.decode(bytes));
    }

    public static void testCoding(List<Long> expList, VarLongEncoder coder) {
        for (var x : expList) {
            byte[] encodedBytes = coder.encode(x);
            var decoded = coder.decode(encodedBytes);
            assertEquals(encodedBytes.length, decoded.bytesCount());
            assertEquals(x, decoded.result());
        }
    }
}