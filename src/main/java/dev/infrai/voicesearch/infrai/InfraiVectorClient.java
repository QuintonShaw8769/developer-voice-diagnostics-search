package dev.infrai.voicesearch.infrai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public final class InfraiVectorClient implements VectorSearchPort {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json;
    private final InfraiProperties properties;
    private final AtomicBoolean collectionReady = new AtomicBoolean();

    public InfraiVectorClient(ObjectMapper json, InfraiProperties properties) {
        this.json = json;
        this.properties = properties;
    }

    @Override
    public synchronized void ensureCollection(int dimension) {
        if (collectionReady.get()) return;
        post("/vector/collection/create", Map.of(
                "collection", properties.collection(),
                "dimension", dimension,
                "metric", "cosine",
                "metadata", Map.of("purpose", "developer voice diagnostics")
        ), "collection-" + properties.collection());
        collectionReady.set(true);
    }

    @Override
    public void upsert(String recordingId, List<Float> embedding, Map<String, Object> metadata) {
        post("/vector/upsert", Map.of(
                "collection", properties.collection(),
                "vectors", List.of(Map.of(
                        "id", recordingId,
                        "embedding", embedding,
                        "metadata", metadata
                ))
        ), "recording-" + recordingId);
    }

    @Override
    public List<JsonNode> query(List<Float> embedding, int topK) {
        JsonNode data = post("/vector/query", Map.of(
                "collection", properties.collection(),
                "embedding", embedding,
                "top_k", topK,
                "filter", Map.of(),
                "include_metadata", true
        ), null);
        JsonNode items = data.path("items");
        if (!items.isArray()) throw new IllegalStateException("Expected data.items in vector response");
        return json.convertValue(items, json.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
    }

    private JsonNode post(String path, Object body, String idempotencyKey) {
        int attempts = properties.retry().maxAttempts();
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(properties.baseUrl() + path))
                        .header("Authorization", "Bearer " + properties.apiKey())
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30));
                if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
                HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

                JsonNode envelope = json.readTree(response.body());
                if (!envelope.path("ok").asBoolean(false)) {
                    if (response.statusCode() == 429 && attempt + 1 < attempts) {
                        pause(response, attempt);
                        continue;
                    }
                    JsonNode error = envelope.path("error");
                    throw new InfraiApiException(error.path("code").asText("request_rejected"),
                            error.path("message").asText("Infrai rejected the request"), response.statusCode());
                }
                if (response.statusCode() >= 500) throw new IOException("Remote service response " + response.statusCode());
                return envelope.path("data");
            } catch (InfraiApiException e) {
                throw e;
            } catch (IOException e) {
                if (attempt + 1 == attempts) throw new IllegalStateException("Infrai transport request failed", e);
                pause(null, attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Request interrupted", e);
            }
        }
        throw new IllegalStateException("Retry loop exhausted");
    }

    private void pause(HttpResponse<?> response, int attempt) {
        long fallback = properties.retry().initialDelayMs() * (1L << attempt);
        long delay = response == null ? fallback : response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1000L).orElse(fallback);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }
}
