package server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class MultiNodeApplication {
    static Map<String, Integer> nodePorts = Map.of(
            "Node1", 8080,
            "Node2", 8081,
            "Node3", 8082
    );

    private static void startNode(String nodeName, int port) {
        ConfigurableApplicationContext context = new SpringApplication(MultiNodeApplication.class)
                .run("--server.port=" + port);

        System.out.println("Node '" + nodeName + "' started on port " + port);
    }

    public static void main(String[] args) {

        for (Map.Entry<String, Integer> entry : nodePorts.entrySet()) {
            startNode(entry.getKey(), entry.getValue());
        }
    }

//    @Bean
//    public String printPort(@Value("${server.port}") int port) {
//        System.out.println("Server running on port: " + port);
//        return "Server running on port: " + port;
//    }
}
