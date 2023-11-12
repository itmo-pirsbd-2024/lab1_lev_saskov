package server;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class RendezvousHashing {

    public static String getNodeForId(List<String> nodes, Long recordId) {
        String id = Long.toString(recordId);
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Node list is empty");
        }

        String selectedNode = null;
        int maxWeight = Integer.MIN_VALUE;

        for (String node : nodes) {
            int weight = calculateWeight(node, id);
            if (weight > maxWeight) {
                maxWeight = weight;
                selectedNode = node;
            }
        }

        return selectedNode;
    }

    private static int calculateWeight(String nodeName, String recordId) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((nodeName + recordId).getBytes(StandardCharsets.UTF_8));

            // Считаем вес как сумму хеш-значения байт
            int weight = 0;
            for (byte b : hash) {
                weight += b;
            }

            return weight;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static void main(String[] args) {
        List<String> nodes = List.of("Node1", "Node2", "Node3");

        Long recordId = 7L;
        String selectedNode = getNodeForId(nodes, recordId);

        System.out.println("Record with ID '" + recordId + "' should be sent to node: " + selectedNode);
    }
}
