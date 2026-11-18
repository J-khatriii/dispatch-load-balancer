package com.jaswant.dispatch.domain;

import java.util.List;

public record DispatchResult(List<Route> routes, List<UnassignedOrder> unassigned) {
}