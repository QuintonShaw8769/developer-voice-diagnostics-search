package dev.infrai.voicesearch.diagnostics;

public record DeveloperRecording(
        String recordingId,
        String transcript,
        String eventType,
        String repository,
        String release,
        String recordedAt
) {}
