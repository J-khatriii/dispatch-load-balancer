package com.jaswant.dispatch.dto;

public record MessageResponse(String message, String status) {

    public static MessageResponse success(String message) {
        return new MessageResponse(message, "success");
    }
}