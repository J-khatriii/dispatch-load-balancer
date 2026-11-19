package com.jaswant.dispatch.dto;

import java.util.List;

public record VehiclePlanDto(
        String vehicleId,
        String currentAddress,
        double capacity,
        double totalLoad,
        String totalDistance,
        List<OrderDto> assignedOrders
) {
}