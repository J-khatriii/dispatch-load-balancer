package com.jaswant.dispatch.mapper;

import com.jaswant.dispatch.domain.DispatchResult;
import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Route;
import com.jaswant.dispatch.domain.UnassignedOrder;
import com.jaswant.dispatch.domain.Vehicle;
import com.jaswant.dispatch.dto.DispatchPlanDto;
import com.jaswant.dispatch.dto.OrderDto;
import com.jaswant.dispatch.dto.UnassignedOrderDto;
import com.jaswant.dispatch.dto.VehicleDto;
import com.jaswant.dispatch.dto.VehiclePlanDto;

import java.util.Locale;

public final class DispatchMapper {

    private DispatchMapper() {
    }

    public static Order toDomain(OrderDto dto) {
        return new Order(dto.orderId(), new Location(dto.latitude(), dto.longitude()),
                dto.address(), dto.packageWeight(), dto.priority());
    }

    public static Vehicle toDomain(VehicleDto dto) {
        return new Vehicle(dto.vehicleId(), dto.capacity(),
                new Location(dto.currentLatitude(), dto.currentLongitude()), dto.currentAddress());
    }

    public static DispatchPlanDto toPlan(DispatchResult result) {
        return new DispatchPlanDto(
                result.routes().stream().map(DispatchMapper::toVehiclePlan).toList(),
                result.unassigned().stream().map(DispatchMapper::toUnassignedDto).toList());
    }

    private static VehiclePlanDto toVehiclePlan(Route route) {
        Vehicle vehicle = route.vehicle();
        return new VehiclePlanDto(vehicle.vehicleId(), vehicle.address(), vehicle.capacity(),
                route.totalLoad(), formatKm(route.totalDistanceKm()),
                route.stops().stream().map(DispatchMapper::toDto).toList());
    }

    private static UnassignedOrderDto toUnassignedDto(UnassignedOrder unassigned) {
        return new UnassignedOrderDto(toDto(unassigned.order()), unassigned.reason());
    }

    private static OrderDto toDto(Order order) {
        return new OrderDto(order.orderId(), order.location().latitude(), order.location().longitude(),
                order.address(), order.weight(), order.priority());
    }

    private static String formatKm(double km) {
        return String.format(Locale.ROOT, "%.2f km", km);
    }
}