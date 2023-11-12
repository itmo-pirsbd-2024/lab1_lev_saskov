package server;

import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpPostExample {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String url = "http://localhost:" + port + "/database/collection";

        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("DELETE");

        con.setDoOutput(true);

        String urlParameters = "name=Colection1&vectorLen=5";

        // Получаем поток вывода, куда будем записывать параметры
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(urlParameters);
            wr.flush();
        }

        // Получаем код ответа
        int responseCode = con.getResponseCode();
        System.out.println("Отправлен POST-запрос по адресу: " + url);
        System.out.println("Код ответа: " + responseCode);

        // Считываем ответ
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            // Выводим ответ
            System.out.println(response.toString());
        }
    }
}
