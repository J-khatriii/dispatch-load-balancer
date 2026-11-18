package com.jaswant.dispatch.strategy;

import com.jaswant.dispatch.domain.DispatchResult;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Route;
import com.jaswant.dispatch.domain.UnassignedOrder;
import com.jaswant.dispatch.domain.UnassignedReason;
import com.jaswant.dispatch.domain.Vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CheapestInsertionStrategy implements DispatchStrategy {

    // Heaviest-first within a priority packs vehicles tighter (first-fit-decreasing).
    private static final Comparator<Order> DISPATCH_ORDER =
            Comparator.comparing(Order::priority).thenComparing(Order::weight, Comparator.reverseOrder());

    private record Placement(Route route, int position, double costKm) {
    }

    @Override
    public DispatchResult dispatch(List<Order> orders, List<Vehicle> vehicles) {
        List<Route> routes = vehicles.stream().map(Route::new).toList();
        double maxCapacity = vehicles.stream().mapToDouble(Vehicle::capacity).max().orElse(0);
        List<UnassignedOrder> unassigned = new ArrayList<>();

        for (Order order : orders.stream().sorted(DISPATCH_ORDER).toList()) {
            bestPlacement(order, routes).ifPresentOrElse(
                    placement -> placement.route().insert(order, placement.position()),
                    () -> unassigned.add(new UnassignedOrder(order, reasonFor(order, maxCapacity))));
        }
        return new DispatchResult(routes, unassigned);
    }

    private static Optional<Placement> bestPlacement(Order order, List<Route> routes) {
        Placement best = null;
        for (Route route : routes) {
            if (!route.canFit(order)) {
                continue;
            }
            for (int position = 0; position <= route.stops().size(); position++) {
                double cost = route.insertionCostKm(order, position);
                if (best == null || cost < best.costKm()) {
                    best = new Placement(route, position, cost);
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static UnassignedReason reasonFor(Order order, double maxCapacity) {
        return order.weight() > maxCapacity
                ? UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY
                : UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY;
    }
}