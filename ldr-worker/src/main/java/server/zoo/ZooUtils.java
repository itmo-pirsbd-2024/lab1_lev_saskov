package server.zoo;

import java.util.HashSet;
import java.util.Set;

public class ZooUtils {
    public static String getDelta(Set<String> bigger, Set<String> smaller) {
        var copy = new HashSet<>(bigger);
        copy.removeAll(smaller);
        return (String) copy.toArray()[0];
    }
}
