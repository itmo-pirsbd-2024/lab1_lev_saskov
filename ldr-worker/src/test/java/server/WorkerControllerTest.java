package server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkerControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private int port;

    @Test
    void addCollection() {
        assertThat(restTemplate.getForObject("http://localhost:" + port + "/database/collection",
                String.class))
    }
}