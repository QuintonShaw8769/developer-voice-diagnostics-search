package dev.infrai.voicesearch.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import dev.infrai.voicesearch.infrai.EmbeddingPort;
import dev.infrai.voicesearch.infrai.VectorSearchPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticSearchServiceTest {
    @Test
    void indexesReleaseContextWithTheTranscriptBeforeMarkingItSearchable() {
        RecordingVectors fake = new RecordingVectors();
        EmbeddingPort embeddings = text -> {
            fake.embeddedText = text;
            return List.of(0.25f, 0.75f);
        };
        var service = new DiagnosticSearchService(embeddings, fake);

        var result = service.index(new DeveloperRecording(
                "call-42", "Rollback followed the failed migration", "release-operation",
                "course-api", "2026.09.1", "2026-09-13T09:30:00Z"));

        assertThat(result.status()).isEqualTo("searchable");
        assertThat(fake.embeddedText).contains("event=release-operation", "repository=course-api",
                "release=2026.09.1", "Rollback followed the failed migration");
        assertThat(fake.dimension).isEqualTo(2);
        assertThat(fake.id).isEqualTo("call-42");
        assertThat(fake.metadata).containsEntry("eventType", "release-operation");
    }

    private static final class RecordingVectors implements VectorSearchPort {
        String embeddedText;
        int dimension;
        String id;
        Map<String, Object> metadata;

        public void ensureCollection(int dimension) { this.dimension = dimension; }
        public void upsert(String id, List<Float> embedding, Map<String, Object> metadata) {
            this.id = id;
            this.metadata = metadata;
        }
        public List<JsonNode> query(List<Float> embedding, int topK) { return List.of(); }
    }
}
