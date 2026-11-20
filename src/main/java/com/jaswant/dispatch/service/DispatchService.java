package com.jaswant.dispatch.service;

import com.jaswant.dispatch.domain.DispatchResult;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Vehicle;
import com.jaswant.dispatch.dto.DispatchPlanDto;
import com.jaswant.dispatch.dto.OrderDto;
import com.jaswant.dispatch.dto.VehicleDto;
import com.jaswant.dispatch.exception.InvalidRequestException;
import com.jaswant.dispatch.exception.NoVehiclesException;
import com.jaswant.dispatch.mapper.DispatchMapper;
import com.jaswant.dispatch.repository.OrderRepository;
import com.jaswant.dispatch.repository.VehicleRepository;
import com.jaswant.dispatch.strategy.CheapestInsertionStrategy;
import com.jaswant.dispatch.strategy.DispatchStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DispatchService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchStrategy strategy;

    public DispatchService(OrderRepository orderRepository, VehicleRepository vehicleRepository,
                           CheapestInsertionStrategy strategy) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.strategy = strategy;
    }

    @Transactional
    public void acceptOrders(List<OrderDto> orders) {
        rejectDuplicates(orders.stream().map(OrderDto::orderId).toList(), "order");
        orderRepository.upsertAll(orders.stream().map(DispatchMapper::toDomain).toList());
    }

    @Transactional
    public void acceptVehicles(List<VehicleDto> vehicles) {
        rejectDuplicates(vehicles.stream().map(VehicleDto::vehicleId).toList(), "vehicle");
        vehicleRepository.upsertAll(vehicles.stream().map(DispatchMapper::toDomain).toList());
    }

    @Transactional(readOnly = true)
    public DispatchPlanDto buildPlan() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        if (vehicles.isEmpty()) {
            throw new NoVehiclesException();
        }
        List<Order> orders = orderRepository.findAll();
        DispatchResult result = strategy.dispatch(orders, vehicles);
        return DispatchMapper.toPlan(result);
    }

    private static void rejectDuplicates(List<String> ids, String kind) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                duplicates.add(id);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new InvalidRequestException(
                    "Duplicate %s IDs in request: %s".formatted(kind, String.join(", ", duplicates)));
        }
    }
}