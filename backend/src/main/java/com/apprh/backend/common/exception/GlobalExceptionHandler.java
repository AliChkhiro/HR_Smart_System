package com.apprh.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest request) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(Instant.now(), e.getStatus().value(), e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "MALFORMED_JSON",
                        "Corps de requête invalide", request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(Instant.now(), HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "Accès refusé", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception e, HttpServletRequest request) {
        log.error("Erreur interne sur {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Erreur interne", request.getRequestURI()));
    }
}
