package server;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class Send {

    public static void sendDataToPort(int port, RequestData RequestData) {

        String url = "http://localhost:" + port + "/database/collection";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RequestData> requestEntity = new HttpEntity<>(RequestData, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);

        System.out.println("Response Code: " + responseEntity.getStatusCode());
        System.out.println("Response Body: " + responseEntity.getBody());
    }

    public static void main(String[] args) {

        RequestData RequestData = new RequestData();
        RequestData.setNodeName("Node1");
        RequestData.setVectorLen(10);

        sendDataToPort(8081, RequestData);
        System.out.println("ok");
//        sendDataToPort(8081, RequestData);
//        System.out.println("ok");
//        sendDataToPort(8082, RequestData);
    }

    public static class RequestData {

        private String nodeName;
        private int vectorLen;

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public int getVectorLen() {
            return vectorLen;
        }

        public void setVectorLen(int vectorLen) {
            this.vectorLen = vectorLen;
        }

        @Override
        public String toString() {
            return "RequestData{" +
                    "nodeName='" + nodeName + '\'' +
                    ", vectorLen=" + vectorLen +
                    '}';
        }
    }
}



