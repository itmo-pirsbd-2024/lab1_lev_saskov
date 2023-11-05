package ldr.server.serialization.my;

import java.util.List;

import org.junit.jupiter.api.Test;

class VarIntEncoderTest {
    @Test
    public void testCoding() {
        List<Integer> expList = List.of(Integer.MAX_VALUE, Integer.MIN_VALUE, -1231231, 123, 0);
        DataEncoder<Integer> coder = new VarIntEncoder();

        CoderTestUtil.testCoding(expList, coder);
    }
}