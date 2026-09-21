package dev.infrai.voicesearch.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/developer-recordings")
public final class DeveloperRecordingController {
    private final DiagnosticSearchService search;

    public DeveloperRecordingController(DiagnosticSearchService search) {
        this.search = search;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DiagnosticSearchService.IngestedRecording index(@RequestBody DeveloperRecording recording) {
        return search.index(recording);
    }

    @PostMapping("/search")
    List<JsonNode> search(@RequestBody SearchRequest request) {
        return search.search(request.query(), request.topK());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalidInput(IllegalArgumentException error) {
        return Map.of("error", error.getMessage());
    }

    record SearchRequest(String query, int topK) {}
}
