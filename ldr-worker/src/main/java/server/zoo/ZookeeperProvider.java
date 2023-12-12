package server.zoo;

import java.io.IOException;

import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperProvider {
    @Bean
    public ZooKeeper zooKeeper(@Value("${zookeeper.host}") String host, @Value("${zookeeper.timeout}") int timeout) throws IOException {
        return new ZooKeeper(host, timeout, null);
    }
}

