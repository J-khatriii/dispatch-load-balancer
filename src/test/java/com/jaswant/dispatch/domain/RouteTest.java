package com.jaswant.dispatch.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RouteTest {

    private static final double DEGREE_KM = new Location(0, 0).distanceKmTo(new Location(0, 1));
    private static final double TOLERANCE = 1e-6;

    private final Route route = new Route(new Vehicle("VEH001", 100, new Location(0, 0), "Depot"));

    private static Order order(String id, double lat, double lon, double weight) {
        return new Order(id, new Location(lat, lon), "Address " + id, weight, Priority.MEDIUM);
    }

    @Test
    void emptyRouteHasNoDistanceOrLoad() {
        assertThat(route.totalDistanceKm()).isZero();
        assertThat(route.totalLoad()).isZero();
    }

    @Test
    void totalDistanceSumsLegsStartingFromVehicleLocation() {
        route.insert(order("A", 0, 1, 10), 0);
        route.insert(order("B", 0, 2, 10), 1);

        assertThat(route.totalDistanceKm()).isCloseTo(2 * DEGREE_KM, within(TOLERANCE));
    }

    @Test
    void totalLoadSumsInsertedWeights() {
        route.insert(order("A", 0, 1, 10), 0);
        route.insert(order("B", 0, 2, 25), 1);

        assertThat(route.totalLoad()).isEqualTo(35);
    }

    @Test
    void orderFitsUpToExactCapacity() {
        route.insert(order("A", 0, 1, 60), 0);

        assertThat(route.canFit(order("B", 0, 2, 40))).isTrue();
        assertThat(route.canFit(order("C", 0, 2, 40.1))).isFalse();
    }

    @Test
    void insertingBeyondCapacityIsRejected() {
        route.insert(order("A", 0, 1, 60), 0);

        assertThatThrownBy(() -> route.insert(order("B", 0, 2, 50), 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void appendingCostsDistanceFromLastStop() {
        route.insert(order("A", 0, 1, 10), 0);

        assertThat(route.insertionCostKm(order("B", 0, 3, 10), 1))
                .isCloseTo(2 * DEGREE_KM, within(TOLERANCE));
    }

    @Test
    void insertingBetweenCollinearStopsIsFree() {
        route.insert(order("A", 0, 1, 10), 0);
        route.insert(order("B", 0, 3, 10), 1);

        assertThat(route.insertionCostKm(order("C", 0, 2, 10), 1))
                .isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    void insertingAtFrontIsMeasuredFromVehicleLocation() {
        route.insert(order("A", 0, 2, 10), 0);

        assertThat(route.insertionCostKm(order("B", 0, 1, 10), 0))
                .isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    void detourCostsExtraDistance() {
        route.insert(order("A", 0, 2, 10), 0);

        assertThat(route.insertionCostKm(order("B", 1, 1, 10), 0)).isPositive();
    }
}