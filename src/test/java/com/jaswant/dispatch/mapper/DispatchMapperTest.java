package com.jaswant.dispatch.mapper;

import com.jaswant.dispatch.domain.DispatchResult;
import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Priority;
import com.jaswant.dispatch.domain.Route;
import com.jaswant.dispatch.domain.UnassignedOrder;
import com.jaswant.dispatch.domain.UnassignedReason;
import com.jaswant.dispatch.domain.Vehicle;
import com.jaswant.dispatch.dto.OrderDto;
import com.jaswant.dispatch.dto.VehicleDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchMapperTest {

    private static final Vehicle DEPOT_VEHICLE = new Vehicle("VEH001", 100, new Location(0, 0), "Depot");

    private static Order order(String id, double lon, double weight, Priority priority) {
        return new Order(id, new Location(0, lon), "Address " + id, weight, priority);
    }

    @Test
    void orderDtoMapsToDomain() {
        var dto = new OrderDto("ORD001", 12.9716, 77.5946, "MG Road", 10.0, Priority.HIGH);

        assertThat(DispatchMapper.toDomain(dto)).isEqualTo(
                new Order("ORD001", new Location(12.9716, 77.5946), "MG Road", 10.0, Priority.HIGH));
    }

    @Test
    void vehicleDtoMapsToDomain() {
        var dto = new VehicleDto("VEH001", 100.0, 12.9716, 77.6413, "Indiranagar");

        assertThat(DispatchMapper.toDomain(dto)).isEqualTo(
                new Vehicle("VEH001", 100.0, new Location(12.9716, 77.6413), "Indiranagar"));
    }

    @Test
    void routeBecomesVehiclePlanWithOrdersInStopSequence() {
        var route = new Route(DEPOT_VEHICLE);
        route.insert(order("A", 1, 10, Priority.HIGH), 0);
        route.insert(order("B", 2, 15, Priority.LOW), 1);

        var plan = DispatchMapper.toPlan(new DispatchResult(List.of(route), List.of()));

        assertThat(plan.dispatchPlan()).singleElement().satisfies(vehiclePlan -> {
            assertThat(vehiclePlan.vehicleId()).isEqualTo("VEH001");
            assertThat(vehiclePlan.currentAddress()).isEqualTo("Depot");
            assertThat(vehiclePlan.capacity()).isEqualTo(100);
            assertThat(vehiclePlan.totalLoad()).isEqualTo(25);
            assertThat(vehiclePlan.totalDistance()).isEqualTo("222.39 km");
            assertThat(vehiclePlan.assignedOrders()).extracting(OrderDto::orderId).containsExactly("A", "B");
        });
    }

    @Test
    void idleVehicleStaysInThePlan() {
        var plan = DispatchMapper.toPlan(new DispatchResult(List.of(new Route(DEPOT_VEHICLE)), List.of()));

        assertThat(plan.dispatchPlan()).singleElement().satisfies(vehiclePlan -> {
            assertThat(vehiclePlan.assignedOrders()).isEmpty();
            assertThat(vehiclePlan.totalLoad()).isZero();
            assertThat(vehiclePlan.totalDistance()).isEqualTo("0.00 km");
        });
    }

    @Test
    void unassignedOrdersCarryTheirReason() {
        var unassigned = new UnassignedOrder(
                order("BIG", 1, 500, Priority.HIGH), UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY);

        var plan = DispatchMapper.toPlan(new DispatchResult(List.of(), List.of(unassigned)));

        assertThat(plan.unassignedOrders()).singleElement().satisfies(item -> {
            assertThat(item.order().orderId()).isEqualTo("BIG");
            assertThat(item.reason()).isEqualTo(UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY);
        });
    }

    @Test
    void distanceUsesDotDecimalRegardlessOfDefaultLocale() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            var route = new Route(DEPOT_VEHICLE);
            route.insert(order("A", 1, 10, Priority.HIGH), 0);

            var plan = DispatchMapper.toPlan(new DispatchResult(List.of(route), List.of()));

            assertThat(plan.dispatchPlan().get(0).totalDistance()).isEqualTo("111.19 km");
        } finally {
            Locale.setDefault(original);
        }
    }
}