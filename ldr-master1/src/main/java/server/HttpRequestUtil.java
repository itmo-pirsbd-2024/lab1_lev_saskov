package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import ldr.client.domen.Embedding;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static server.RendezvousHashing.getNodeForId;

public class HttpRequestUtil {
    public static String sendRequest(String url, String method) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpRequestBase request;

        switch (method.toUpperCase()) {
            case "GET":
                request = new HttpGet(url);
                break;
            case "POST":
                request = new HttpPost(url);
                //     ((HttpPost) request).setEntity(new StringEntity(requestBody));
                break;
            case "PUT":
                request = new HttpPut(url);
                //    ((HttpPut) request).setEntity(new StringEntity(requestBody));
                break;
            case "DELETE":
                request = new HttpDelete(url);
                //   ((HttpDelete) request).setEntity(new StringEntity(requestBody));
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
        String url = "http://localhost:" + port + "/database/collection" +
                "?name=" + name + "&vectorLen=" + vectorLen;
        String response = sendRequest(url, "POST");
        System.out.println("Response: " + response);
        return response;
    }

    public static String deleteCollection(int port, String name) throws IOException {
        String url = "http://localhost:" + port + "/database/collection" + "?" + "name=" + name;
        String response = sendRequest(url, "DELETE");
        System.out.println("Response: " + response);
        return response;
    }

    public static String renameCollection(int port, String oldName, String newName) throws IOException {
        String url = "http://localhost:" + port + "/database/collection" +
                "?oldName=" + oldName + "&newName=" + newName;
        String response = sendRequest(url, "PUT");
        System.out.println("Response: " + response);
        return response;
    }

    public static String addToCollection(Embedding embedding, String collectionName) throws IOException {
        Map<String, Integer> config = MultiNodeApplication.nodePorts;
        List<String> nodes = new ArrayList<>(config.keySet());
        Long recordId = embedding.id();
        String selectedNode = getNodeForId(nodes, recordId);
        int port = config.get(selectedNode);
        String response = sendBodyRequest("COLLECTION_TEST", embedding, port);
        System.out.println("Response: " + response);
        return response;
    }

    public static String deleteFromCollection(long id, String collectionName) throws IOException {
        Map<String, Integer> config = MultiNodeApplication.nodePorts;
        List<String> nodes = new ArrayList<>(config.keySet());
        String selectedNode = getNodeForId(nodes, id);
        int port = config.get(selectedNode);

        String url = "http://localhost:" + port + "/database/collection/" + collectionName +
                "?id=" + id + "&collectionName=" + collectionName;
        String response = sendRequest(url, "DELETE");
        System.out.println("Response: " + response);
        return response;
    }

    public static String sendBodyRequest(String collectionName, Embedding embedding, int port) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = "http://localhost:" + port + "/database/collection/" + collectionName;
            HttpPut putRequest = new HttpPut(url);

            // Преобразование Embedding объекта в JSON-строку
            String requestBody = new ObjectMapper().writeValueAsString(embedding);
            StringEntity stringEntity = new StringEntity(requestBody);
            putRequest.setEntity(stringEntity);
            putRequest.setHeader("Content-Type", "application/json");

            // Выполнение PUT-запроса и получение ответа
            HttpEntity responseEntity = httpClient.execute(putRequest).getEntity();
            String responseString = org.apache.http.util.EntityUtils.toString(responseEntity);
            return responseString;
        }
    }

    public static void main(String[] args) throws IOException {
        Embedding embedding = new Embedding(101, new double[]{13.0, 17.5, 15.0},
                Map.of("color", "green", "size", "big"));

        Map<String, Integer> config = MultiNodeApplication.nodePorts;
        List<String> nodes = new ArrayList<>(config.keySet());

        for (String node : nodes) {
            int port = config.get(node);
            System.out.println(port);
            String responseCreateCollection = createCollection(port, "COLECTION_TEST", 10);
            String responseRenameCollection = renameCollection(port, "COLECTION_TEST", "COLECTION_TEST_NEW");
            String responseDeleteCollection = deleteCollection(port, "COLECTION_TEST");
        }

        String responseAddToCollection = addToCollection(embedding, "COLECTION_TEST");
        String responseDeleteFromCollection = deleteFromCollection(106, "COLECTION_TEST");

    }
}




