package dev.infrai.voicesearch.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import dev.infrai.voicesearch.infrai.EmbeddingPort;
import dev.infrai.voicesearch.infrai.VectorSearchPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public final class DiagnosticSearchService {
    private final EmbeddingPort embeddings;
    private final VectorSearchPort vectors;

    public DiagnosticSearchService(EmbeddingPort embeddings, VectorSearchPort vectors) {
        this.embeddings = embeddings;
        this.vectors = vectors;
    }

    public IngestedRecording index(DeveloperRecording recording) {
        requireText(recording.recordingId(), "recordingId");
        requireText(recording.transcript(), "transcript");
        requireText(recording.eventType(), "eventType");
        requireText(recording.repository(), "repository");
        requireText(recording.release(), "release");
        requireText(recording.recordedAt(), "recordedAt");
        List<Float> embedding = embeddings.embed(searchableLesson(recording));
        vectors.ensureCollection(embedding.size());
        vectors.upsert(recording.recordingId(), embedding, Map.of(
                "transcript", recording.transcript(),
                "eventType", recording.eventType(),
                "repository", recording.repository(),
                "release", recording.release(),
                "recordedAt", recording.recordedAt()
        ));
        return new IngestedRecording(recording.recordingId(), "searchable");
    }

    public List<JsonNode> search(String query, int topK) {
        requireText(query, "query");
        if (topK < 1 || topK > 20) throw new IllegalArgumentException("topK must be between 1 and 20");
        return vectors.query(embeddings.embed(query), topK);
    }

    static String searchableLesson(DeveloperRecording recording) {
        return "event=" + recording.eventType() + " repository=" + recording.repository()
                + " release=" + recording.release() + " transcript=" + recording.transcript();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    public record IngestedRecording(String recordingId, String status) {}
}
