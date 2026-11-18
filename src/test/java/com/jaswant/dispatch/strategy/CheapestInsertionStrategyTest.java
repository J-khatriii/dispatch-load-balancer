package com.jaswant.dispatch.strategy;

import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Priority;
import com.jaswant.dispatch.domain.Route;
import com.jaswant.dispatch.domain.UnassignedReason;
import com.jaswant.dispatch.domain.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheapestInsertionStrategyTest {

    private final CheapestInsertionStrategy strategy = new CheapestInsertionStrategy();

    private static Order order(String id, double lon, double weight, Priority priority) {
        return new Order(id, new Location(0, lon), "Address " + id, weight, priority);
    }

    private static Vehicle vehicle(String id, double capacity, double lon) {
        return new Vehicle(id, capacity, new Location(0, lon), "Depot " + id);
    }

    private static List<String> stopIds(Route route) {
        return route.stops().stream().map(Order::orderId).toList();
    }

    @Test
    void highPriorityOrdersGetCapacityFirst() {
        var result = strategy.dispatch(
                List.of(order("LOW1", 1, 30, Priority.LOW), order("HIGH1", 2, 30, Priority.HIGH)),
                List.of(vehicle("V1", 50, 0)));

        assertThat(stopIds(result.routes().get(0))).containsExactly("HIGH1");
        assertThat(result.unassigned()).singleElement().satisfies(unassigned -> {
            assertThat(unassigned.order().orderId()).isEqualTo("LOW1");
            assertThat(unassigned.reason()).isEqualTo(UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY);
        });
    }

    @Test
    void orderHeavierThanEveryVehicleIsUnassignedWithReason() {
        var result = strategy.dispatch(
                List.of(order("BIG", 1, 100, Priority.HIGH)),
                List.of(vehicle("V1", 50, 0), vehicle("V2", 80, 5)));

        assertThat(result.unassigned()).singleElement()
                .extracting("reason")
                .isEqualTo(UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY);
    }

    @Test
    void orderGoesToNearestVehicle() {
        var result = strategy.dispatch(
                List.of(order("A", 9, 10, Priority.HIGH)),
                List.of(vehicle("V1", 100, 0), vehicle("V2", 100, 10)));

        assertThat(stopIds(result.routes().get(0))).isEmpty();
        assertThat(stopIds(result.routes().get(1))).containsExactly("A");
    }

    @Test
    void ordersAtTheSameLocationShareOneVehicle() {
        var result = strategy.dispatch(
                List.of(order("A", 1, 10, Priority.HIGH), order("B", 1, 10, Priority.HIGH)),
                List.of(vehicle("V1", 100, 0), vehicle("V2", 100, 5)));

        assertThat(stopIds(result.routes().get(0))).containsExactlyInAnyOrder("A", "B");
        assertThat(stopIds(result.routes().get(1))).isEmpty();
    }

    @Test
    void stopsAreInsertedInTheCheapestPosition() {
        var result = strategy.dispatch(
                List.of(order("A", 3, 30, Priority.MEDIUM),
                        order("B", 1, 20, Priority.MEDIUM),
                        order("C", 2, 10, Priority.MEDIUM)),
                List.of(vehicle("V1", 100, 0)));

        assertThat(stopIds(result.routes().get(0))).containsExactly("B", "C", "A");
    }
}