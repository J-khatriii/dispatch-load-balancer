package com.jaswant.dispatch.strategy;

import com.jaswant.dispatch.domain.DispatchResult;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Vehicle;

import java.util.List;

public interface DispatchStrategy {

    DispatchResult dispatch(List<Order> orders, List<Vehicle> vehicles);
}