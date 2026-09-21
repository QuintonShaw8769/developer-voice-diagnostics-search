package dev.infrai.voicesearch.infrai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public interface VectorSearchPort {
    void ensureCollection(int dimension);
    void upsert(String recordingId, List<Float> embedding, Map<String, Object> metadata);
    List<JsonNode> query(List<Float> embedding, int topK);
}
