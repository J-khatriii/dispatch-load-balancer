package com.jaswant.dispatch.strategy;

import com.jaswant.dispatch.domain.DispatchResult;
import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Priority;
import com.jaswant.dispatch.domain.UnassignedOrder;
import com.jaswant.dispatch.domain.UnassignedReason;
import com.jaswant.dispatch.domain.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SampleDataDispatchTest {

    private static final Map<String, Location> PLACES = Map.of(
            "CONNAUGHT_PLACE", new Location(28.6139, 77.2090),
            "KAROL_BAGH", new Location(28.7041, 77.1025),
            "NOIDA", new Location(28.5355, 77.3910),
            "GURGAON", new Location(28.4595, 77.0266));

    private static final List<Order> ORDERS = """
            ORD001 CONNAUGHT_PLACE 15 HIGH
            ORD002 CONNAUGHT_PLACE 10 MEDIUM
            ORD003 KAROL_BAGH 20 LOW
            ORD004 NOIDA 25 HIGH
            ORD005 GURGAON 30 MEDIUM
            ORD006 CONNAUGHT_PLACE 40 LOW
            ORD007 KAROL_BAGH 20 HIGH
            ORD008 NOIDA 25 HIGH
            ORD009 GURGAON 15 MEDIUM
            ORD010 CONNAUGHT_PLACE 30 LOW
            ORD011 KAROL_BAGH 20 HIGH
            ORD012 NOIDA 10 MEDIUM
            ORD013 GURGAON 25 HIGH
            ORD014 CONNAUGHT_PLACE 15 LOW
            ORD015 KAROL_BAGH 20 HIGH
            ORD016 NOIDA 30 LOW
            ORD017 GURGAON 15 MEDIUM
            ORD018 CONNAUGHT_PLACE 10 HIGH
            ORD019 KAROL_BAGH 20 LOW
            ORD020 NOIDA 30 HIGH
            ORD021 CONNAUGHT_PLACE 25 MEDIUM
            ORD022 KAROL_BAGH 20 LOW
            ORD023 NOIDA 30 HIGH
            ORD024 GURGAON 15 LOW
            ORD025 CONNAUGHT_PLACE 20 MEDIUM
            ORD026 KAROL_BAGH 25 HIGH
            ORD027 NOIDA 20 MEDIUM
            ORD028 GURGAON 15 LOW
            ORD029 CONNAUGHT_PLACE 30 HIGH
            ORD030 KAROL_BAGH 20 LOW
            """.lines().map(SampleDataDispatchTest::parseOrder).toList();

    private static final List<Vehicle> VEHICLES = List.of(
            vehicle("VEH001", 100, "KAROL_BAGH"),
            vehicle("VEH002", 80, "NOIDA"),
            vehicle("VEH003", 120, "GURGAON"),
            vehicle("VEH004", 90, "CONNAUGHT_PLACE"),
            vehicle("VEH005", 110, "KAROL_BAGH"));

    private final CheapestInsertionStrategy strategy = new CheapestInsertionStrategy();
    private final DispatchResult result = strategy.dispatch(ORDERS, VEHICLES);

    private static Order parseOrder(String line) {
        String[] fields = line.trim().split("\\s+");
        return new Order(fields[0], PLACES.get(fields[1]), fields[1],
                Double.parseDouble(fields[2]), Priority.valueOf(fields[3]));
    }

    private static Vehicle vehicle(String id, double capacity, String place) {
        return new Vehicle(id, capacity, PLACES.get(place), place);
    }

    private static List<List<String>> plan(DispatchResult dispatchResult) {
        return dispatchResult.routes().stream()
                .map(route -> route.stops().stream().map(Order::orderId).toList())
                .toList();
    }

    @Test
    void everyOrderIsAccountedForExactlyOnce() {
        List<String> placed = Stream.concat(
                        result.routes().stream().flatMap(route -> route.stops().stream()),
                        result.unassigned().stream().map(UnassignedOrder::order))
                .map(Order::orderId)
                .toList();

        assertThat(placed).containsExactlyInAnyOrderElementsOf(ORDERS.stream().map(Order::orderId).toList());
    }

    @Test
    void noVehicleIsOverloaded() {
        assertThat(result.routes()).allSatisfy(route ->
                assertThat(route.totalLoad()).isLessThanOrEqualTo(route.vehicle().capacity()));
    }

    @Test
    void everyHighPriorityOrderIsAssigned() {
        assertThat(result.unassigned()).noneMatch(unassigned -> unassigned.order().priority() == Priority.HIGH);
    }

    @Test
    void unassignedOrdersTrulyHaveNoRoomAnywhere() {
        assertThat(result.unassigned()).isNotEmpty().allSatisfy(unassigned -> {
            assertThat(unassigned.reason()).isEqualTo(UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY);
            assertThat(result.routes()).noneMatch(route -> route.canFit(unassigned.order()));
        });
    }

    @Test
    void dispatchIsDeterministic() {
        assertThat(plan(strategy.dispatch(ORDERS, VEHICLES))).isEqualTo(plan(result));
    }
}