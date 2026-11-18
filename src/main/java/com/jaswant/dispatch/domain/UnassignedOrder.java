package com.jaswant.dispatch.domain;

public record UnassignedOrder(Order order, UnassignedReason reason) {
}