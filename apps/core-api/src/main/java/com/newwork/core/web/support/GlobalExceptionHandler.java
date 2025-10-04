package com.newwork.core.web.support;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Etags.VersionMismatchException.class)
    public ResponseEntity<Object> handleVersion(Etags.VersionMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error","version_mismatch","currentVersion", ex.current));
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleRSE(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString()));
    }
}
