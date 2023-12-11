package server;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class RendezvousHashing {
    private final List<String> nodes;
    private final MessageDigest messageDigest;

    public RendezvousHashing(List<String> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Node list is empty");
        }
        this.nodes = nodes;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public String getNodeForId(Long recordId) {
        String id = Long.toString(recordId);
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

    private int calculateWeight(String nodeName, String recordId) {
        byte[] hash = messageDigest.digest((nodeName + recordId).getBytes(StandardCharsets.UTF_8));

        // Считаем вес как сумму хеш-значения байт
        int weight = 0;
        for (byte b : hash) {
            weight += b;
        }

        return weight;
    }

    public static void main(String[] args) {
        List<String> nodes = List.of("Node1", "Node2", "Node3");

        RendezvousHashing rendezvousHashing = new RendezvousHashing(nodes);
        Long recordId = 7L;
        String selectedNode = rendezvousHashing.getNodeForId(recordId);

        System.out.println("Record with ID '" + recordId + "' should be sent to node: " + selectedNode);
    }
}
