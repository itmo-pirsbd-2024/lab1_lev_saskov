package ldr.server.serialization.my;

import java.util.List;

import org.junit.jupiter.api.Test;

class StringEncoderTest {
    @Test
    public void testCoding() {
        List<String> expList = List.of("Lala2", "Lallre", "Я могучий", "12345 вышел зайчик");
        DataEncoder<String> coder = new StringEncoder();
        CoderTestUtil.testCoding(expList, coder);
    }
}