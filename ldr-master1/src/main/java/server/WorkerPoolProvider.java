package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import client.IWorkerClient;
import client.WorkerClient;

@Configuration
public class WorkerPoolProvider {
    @Bean
    public WorkerPool workerPool(@Value("#{${workers.configs}}") Map<String, String> workersConfigs) {
        List<IWorkerClient> workers = new ArrayList<>(workersConfigs.size());
        for (var wc : workersConfigs.entrySet()) {
            IWorkerClient workerClient = new WorkerClient(wc.getValue(), wc.getKey());
            workers.add(workerClient);
        }
        return new WorkerPool(workers);
    }
}
