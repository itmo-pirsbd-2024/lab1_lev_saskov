package server;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static server.RendezvousHashing.getNodeForId;

public class HttpRequestUtil {
    public static String sendRequest(String url, String method, String requestBody) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpRequestBase request;

        switch (method.toUpperCase()) {
            case "GET":
                request = new HttpGet(url);
                break;
            case "POST":
                request = new HttpPost(url);
                ((HttpPost) request).setEntity(new StringEntity(requestBody));
                break;
            case "PUT":
                request = new HttpPut(url);
                ((HttpPut) request).setEntity(new StringEntity(requestBody));
                break;
            case "DELETE":
                request = new HttpDelete(url);
                // ((HttpPut) request).setEntity(new StringEntity(requestBody));
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        request.setHeader("Content-Type", "application/x-www-form-urlencoded");

        HttpResponse response = httpClient.execute(request);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            return result.toString();
        }
    }

    public static String createCollection(int port, String name, int vectorLen) throws IOException {
        String url = "http://localhost:" + port + "/database/collection";
        String requestBody = "name=" + name + "&vectorLen=" + vectorLen;
        String response = sendRequest(url, "POST", requestBody);
        System.out.println("Response: " + response);
        return response;
    }

    public static String deleteCollection(int port, String name) throws IOException {
        String url = "http://localhost:" + port + "/database/collection";
        String requestBody = "name=" + name;
        String response = sendRequest(url, "DELETE", requestBody);
        System.out.println("Response: " + response);
        return response;
    }

    public static String renameCollection(int port, String oldName, String newName) throws IOException {
        String url = "http://localhost:" + port + "/database/collection";
        String requestBody = "oldName=" + oldName + "&newName=" + newName;
        String response = sendRequest(url, "PUT", requestBody);
        System.out.println("Response: " + response);
        return response;
    }

    public static String addToCollection(Embedding embedding, String collectionName) throws IOException {
        Map<String, Integer> config = MultiNodeApplication.nodePorts;
        List<String> nodes = new ArrayList<>(config.keySet());
        Long recordId = embedding.id();
        String selectedNode = getNodeForId(nodes, recordId);

        int port = config.get(selectedNode);
        String url = "http://localhost:" + port + "/database/collection/" + collectionName;
        String requestBody = "embedding=" + embedding + "&collectionName=" + collectionName;
        String response = sendRequest(url, "PUT", requestBody);
        System.out.println("Response: " + response);
        return response;
    }

    public static String deleteFromCollection(long id, String collectionName) throws IOException {
        Map<String, Integer> config = MultiNodeApplication.nodePorts;
        List<String> nodes = new ArrayList<>(config.keySet());
        String selectedNode = getNodeForId(nodes, id);
        int port = config.get(selectedNode);

        String url = "http://localhost:" + port + "/database/collection/" + collectionName;
        String requestBody = "id=" + id + "&collectionName=" + collectionName;
        String response = sendRequest(url, "DELETE", requestBody);
        System.out.println("Response: " + response);
        return response;
    }


    public static void main(String[] args) throws IOException {
//        try {
//            // Пример использования
//            String url = "http://localhost:8081/database/collection";
//            String method = "POST";
//            String requestBody = "name=Colection1&vectorLen=5";
//
//            String response = sendRequest(url, method, requestBody);
//            System.out.println("Response: " + response);
//        } catch (IOException e) {
//            e.printStackTrace();
        Embedding embedding = new Embedding(101, new double[]{13.0, 17.5, 15.0},
                Map.of("color", "green", "size", "big"));

        String response = createCollection(8081, "COLECTION_TEST", 10);
        String response_1 = renameCollection(8081, "COLECTION_TEST", "COLECTION_TEST_NEW");
        String response_2 = deleteCollection(8081, "COLECTION_TEST");
        String response_3 = addToCollection(embedding, "COLECTION_TEST");
        String response_4 = deleteFromCollection(8081, "COLECTION_TEST");
    }
}




