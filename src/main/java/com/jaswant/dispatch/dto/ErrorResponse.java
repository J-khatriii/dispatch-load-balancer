package com.jaswant.dispatch.dto;

import java.util.List;

public record ErrorResponse(String status, String message, List<String> details) {

    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse("error", message, details);
    }
}