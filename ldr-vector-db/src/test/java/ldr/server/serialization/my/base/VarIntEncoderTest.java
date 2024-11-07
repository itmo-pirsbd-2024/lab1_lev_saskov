package ldr.server.serialization.my.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class VarIntEncoderTest {
    @Test
    public void testCoding() {
        List<Integer> expList = List.of(Integer.MAX_VALUE, Integer.MIN_VALUE, -1231231, 123, 0);
        VarIntEncoder coder = new VarIntEncoder();
        testCoding(expList, coder);
    }

    public static void testCoding(List<Integer> expList, VarIntEncoder coder) {
        for (var x : expList) {
            byte[] encodedBytes = coder.encode(x);
            var decoded = coder.decode(encodedBytes);
            assertEquals(encodedBytes.length, decoded.bytesCount());
            assertEquals(x, decoded.result());
        }
    }
}