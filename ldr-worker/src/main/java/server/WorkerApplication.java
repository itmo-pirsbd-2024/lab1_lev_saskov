package server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerApplication {
//    public static IWorkerClient startNode(String nodeName, int port) {
//        new SpringApplication(WorkerApplication.class)
//                .run("--server.port=" + port, "--database.location=" + nodeName);
//        return new WorkerClient("http://localhost:" + port + "/database/collection", nodeName);
//    }

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
