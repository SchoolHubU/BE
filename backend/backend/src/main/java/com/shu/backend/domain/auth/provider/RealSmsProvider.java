package com.shu.backend.domain.auth.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shu.backend.domain.auth.setting.SensProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;


@Profile("prod")
@Component
@RequiredArgsConstructor
public class RealSmsProvider implements SmsProvider {

    private static final String HOST = "https://sens.apigw.ntruss.com";

    private final SensProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void send(String phoneNumber, String message) {
        try {
            long timestamp = System.currentTimeMillis();
            String uri = "/sms/v2/services/" + props.serviceId() + "/messages";
            String url = HOST + uri;

            Map<String, Object> body = Map.of(
                    "type", "SMS",
                    "contentType", "COMM",
                    "countryCode", "82",
                    "from", props.from(),
                    "content", message,
                    "messages", List.of(
                            Map.of("to", phoneNumber)
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-ncp-apigw-timestamp", String.valueOf(timestamp));
            headers.set("x-ncp-iam-access-key", props.accessKey());
            headers.set(
                    "x-ncp-apigw-signature-v2",
                    createSignature("POST", uri, timestamp)
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(jsonBody, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "SMS send failed: " + response.getStatusCode() + " / " + response.getBody()
                );
            }

        } catch (Exception e) {
            // 🔥 여기서 프로젝트 공통 예외로 감싸는 걸 권장
            throw new RuntimeException("SMS 전송 중 오류 발생", e);
        }
    }

    private String createSignature(String method, String uri, long timestamp) throws Exception {
        String message = method + " " + uri + "\n" +
                timestamp + "\n" +
                props.accessKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                props.secretKey().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rawHmac);
    }
}