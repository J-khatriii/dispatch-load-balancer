package com.jaswant.dispatch.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Route {

    private final Vehicle vehicle;
    private final List<Order> stops = new ArrayList<>();
    private double load;

    public Route(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public boolean canFit(Order order) {
        return load + order.weight() <= vehicle.capacity();
    }

    public double insertionCostKm(Order order, int position) {
        Location previous = position == 0 ? vehicle.location() : stops.get(position - 1).location();
        double toNew = previous.distanceKmTo(order.location());
        if (position == stops.size()) {
            return toNew;
        }
        Location next = stops.get(position).location();
        return toNew + order.location().distanceKmTo(next) - previous.distanceKmTo(next);
    }

    public void insert(Order order, int position) {
        if (!canFit(order)) {
            throw new IllegalStateException(
                    "Order %s exceeds remaining capacity of vehicle %s".formatted(order.orderId(), vehicle.vehicleId()));
        }
        stops.add(position, order);
        load += order.weight();
    }

    public double totalDistanceKm() {
        double total = 0;
        Location previous = vehicle.location();
        for (Order stop : stops) {
            total += previous.distanceKmTo(stop.location());
            previous = stop.location();
        }
        return total;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public List<Order> stops() {
        return Collections.unmodifiableList(stops);
    }

    public double totalLoad() {
        return load;
    }
}