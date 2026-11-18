package com.jaswant.dispatch.domain;

public record Order(String orderId, Location location, String address, double weight, Priority priority) {
}