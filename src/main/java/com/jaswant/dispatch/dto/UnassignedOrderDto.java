package com.jaswant.dispatch.dto;

import com.jaswant.dispatch.domain.UnassignedReason;

public record UnassignedOrderDto(OrderDto order, UnassignedReason reason) {
}