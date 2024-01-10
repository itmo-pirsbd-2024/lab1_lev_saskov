package server.naming;

import java.util.Set;

public class Utils {
    public static String getInternalCollection(String collection, int domain) {
        return collection + "_" + domain;
    }

    public static String getDomainCollectionRegex(Set<Integer> domains) {
        StringBuilder regexBuilder = new StringBuilder(".*_(");
        for (int domain : domains) {
            regexBuilder.append(domain).append("|");
        }
        regexBuilder.deleteCharAt(regexBuilder.length() - 1).append(")");
        return regexBuilder.toString();
    }
}
