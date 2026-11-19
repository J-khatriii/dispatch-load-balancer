package com.jaswant.dispatch.dto;

import java.util.List;

public record DispatchPlanDto(List<VehiclePlanDto> dispatchPlan, List<UnassignedOrderDto> unassignedOrders) {
}