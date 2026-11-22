package com.jaswant.dispatch.controller;

import com.jaswant.dispatch.dto.ErrorResponse;
import com.jaswant.dispatch.exception.InvalidRequestException;
import com.jaswant.dispatch.exception.NoVehiclesException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
                .sorted()
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody() {
        return respond(HttpStatus.BAD_REQUEST,
                "Request body is missing, malformed, or contains an invalid value "
                        + "(priority must be HIGH, MEDIUM or LOW)", List.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(NoVehiclesException.class)
    ResponseEntity<ErrorResponse> handleNoVehicles(NoVehiclesException ex) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(ErrorResponse.of(message, details));
    }
}