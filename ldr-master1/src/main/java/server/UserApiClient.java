package server;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

public class UserApiClient {

    public static void main(String[] args) throws Exception {
        String url = "http://localhost:8080/users/create";

        // Создаем объект HttpClient
        HttpClient httpClient = HttpClients.createDefault();

        // Создаем объект HttpPost с указанным URL
        HttpPost httpPost = new HttpPost(url);

        // Создаем объект User для передачи данных
        User user = new User("John Doe", 25);

        // Преобразуем объект User в JSON-строку
        String requestBody = "{\"name\":\"" + "Joe" +"}";

        // Устанавливаем заголовок Content-Type как application/json
        httpPost.setHeader("Content-Type", "application/json");

        // Устанавливаем JSON-строку в качестве тела запроса
        httpPost.setEntity(new StringEntity(requestBody));

        // Выполняем запрос и получаем ответ
        HttpResponse response = httpClient.execute(httpPost);

        // Выводим статус ответа
        System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
    }
}
