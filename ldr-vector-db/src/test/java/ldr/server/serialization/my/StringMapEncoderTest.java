package ldr.server.serialization.my;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StringMapEncoderTest {
    @Test
    public void testCoding() {
        var expList = List.of(
                Map.of("key1", "val1", "key2", "val2"),
                Map.of("adadafa", "faffscsd", "ffaadadaf4554", "8398292edvssvsv",
                        "Я русский", "Иду до конца")
        );
        DataEncoder<Map<String, String>> coder = new StringMapEncoder();
        CoderTestUtil.testCoding(expList, coder);
    }
}