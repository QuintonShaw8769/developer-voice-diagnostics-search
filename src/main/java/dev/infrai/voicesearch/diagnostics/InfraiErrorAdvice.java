package dev.infrai.voicesearch.diagnostics;

import dev.infrai.voicesearch.infrai.InfraiApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public final class InfraiErrorAdvice {
    @ExceptionHandler(InfraiApiException.class)
    ResponseEntity<Map<String, String>> rejected(InfraiApiException error) {
        int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(status).body(Map.of("code", error.code(), "message", error.getMessage()));
    }
}
