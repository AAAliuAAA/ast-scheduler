package com.ast.scheduler.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class AgentStreamService {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AgentStreamService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public Flux<String> streamOpenClaw(String gatewayUrl, String message, String sessionType, String token) {
        log.info("Starting OpenClaw stream - URL: {}, session: {}, message: {}", gatewayUrl, sessionType, message);

        Map<String, Object> request = Map.of(
                "model", "openclaw:".concat(sessionType),
                "stream", true,
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        var requestSpec = webClient.post()
                .uri(gatewayUrl + "/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream");

        if (token != null && !token.isEmpty()) {
            requestSpec = requestSpec.header("Authorization", "Bearer " + token);
            log.info("Token added to request");
        }

        return requestSpec
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(byte[].class)
                .map(bytes -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
                .doOnNext(line -> log.info("Received line: {}", line))
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .filter(line -> line.startsWith("data: "))
                .doOnNext(line -> log.info("Filtered data line: {}", line))
                .map(line -> line.substring(6))
                .filter(data -> !"[DONE]".equals(data))
                .mapNotNull(this::parseContent)
                .doOnNext(content -> log.info("Parsed content: {}", content))
                .doOnError(error -> log.error("Stream error: ", error))
                .doOnComplete(() -> log.info("Stream completed"));
    }

    private String parseContent(String data) {
        try {
            JsonNode json = objectMapper.readTree(data);
            JsonNode delta = json.path("choices").get(0).path("delta").path("content");
            return delta.isMissingNode() ? null : delta.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
