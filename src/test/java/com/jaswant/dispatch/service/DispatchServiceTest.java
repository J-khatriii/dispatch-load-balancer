package com.jaswant.dispatch.service;

import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Priority;
import com.jaswant.dispatch.domain.UnassignedReason;
import com.jaswant.dispatch.domain.Vehicle;
import com.jaswant.dispatch.dto.OrderDto;
import com.jaswant.dispatch.dto.VehicleDto;
import com.jaswant.dispatch.dto.VehiclePlanDto;
import com.jaswant.dispatch.exception.InvalidRequestException;
import com.jaswant.dispatch.exception.NoVehiclesException;
import com.jaswant.dispatch.mapper.DispatchMapper;
import com.jaswant.dispatch.repository.OrderRepository;
import com.jaswant.dispatch.repository.VehicleRepository;
import com.jaswant.dispatch.strategy.CheapestInsertionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private DispatchService service;

    @BeforeEach
    void setUp() {
        service = new DispatchService(orderRepository, vehicleRepository, new CheapestInsertionStrategy());
    }

    private static OrderDto orderDto(String id) {
        return new OrderDto(id, 12.9716, 77.5946, "MG Road", 10.0, Priority.HIGH);
    }

    private static VehicleDto vehicleDto(String id) {
        return new VehicleDto(id, 100.0, 12.9716, 77.6413, "Indiranagar");
    }

    private static Order order(String id, double lon, double weight) {
        return new Order(id, new Location(0, lon), "Address " + id, weight, Priority.MEDIUM);
    }

    private static Vehicle vehicle(String id, double capacity, double lon) {
        return new Vehicle(id, capacity, new Location(0, lon), "Depot " + id);
    }

    @Test
    void acceptOrdersUpsertsTheMappedOrders() {
        service.acceptOrders(List.of(orderDto("ORD001"), orderDto("ORD002")));

        verify(orderRepository).upsertAll(List.of(
                DispatchMapper.toDomain(orderDto("ORD001")), DispatchMapper.toDomain(orderDto("ORD002"))));
    }

    @Test
    void acceptOrdersRejectsDuplicateIdsBeforeTouchingTheDatabase() {
        var request = List.of(orderDto("ORD001"), orderDto("ORD002"), orderDto("ORD001"));

        assertThatThrownBy(() -> service.acceptOrders(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("ORD001")
                .hasMessageNotContaining("ORD002");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void acceptVehiclesUpsertsTheMappedVehicles() {
        service.acceptVehicles(List.of(vehicleDto("VEH001"), vehicleDto("VEH002")));

        verify(vehicleRepository).upsertAll(List.of(
                DispatchMapper.toDomain(vehicleDto("VEH001")), DispatchMapper.toDomain(vehicleDto("VEH002"))));
    }

    @Test
    void acceptVehiclesRejectsDuplicateIdsBeforeTouchingTheDatabase() {
        var request = List.of(vehicleDto("VEH001"), vehicleDto("VEH001"));

        assertThatThrownBy(() -> service.acceptVehicles(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("VEH001");
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void buildPlanFailsFastWhenNoVehiclesAreRegistered() {
        when(vehicleRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.buildPlan()).isInstanceOf(NoVehiclesException.class);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void buildPlanAssignsStoredOrdersToTheNearestStoredVehicle() {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle("V1", 100, 0), vehicle("V2", 100, 10)));
        when(orderRepository.findAll()).thenReturn(List.of(order("NEAR_V1", 1, 10), order("NEAR_V2", 9, 10)));

        var plan = service.buildPlan();

        assertThat(plan.dispatchPlan()).extracting(VehiclePlanDto::vehicleId).containsExactly("V1", "V2");
        assertThat(plan.dispatchPlan().get(0).assignedOrders()).extracting(OrderDto::orderId)
                .containsExactly("NEAR_V1");
        assertThat(plan.dispatchPlan().get(1).assignedOrders()).extracting(OrderDto::orderId)
                .containsExactly("NEAR_V2");
        assertThat(plan.unassignedOrders()).isEmpty();
    }

    @Test
    void buildPlanWithNoOrdersKeepsEveryVehicleIdle() {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle("V1", 100, 0)));
        when(orderRepository.findAll()).thenReturn(List.of());

        var plan = service.buildPlan();

        assertThat(plan.dispatchPlan()).singleElement()
                .satisfies(vehiclePlan -> assertThat(vehiclePlan.assignedOrders()).isEmpty());
        assertThat(plan.unassignedOrders()).isEmpty();
    }

    @Test
    void buildPlanReportsOrdersThatCannotFit() {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle("V1", 50, 0)));
        when(orderRepository.findAll()).thenReturn(List.of(order("BIG", 1, 80)));

        var plan = service.buildPlan();

        assertThat(plan.unassignedOrders()).singleElement().satisfies(unassigned -> {
            assertThat(unassigned.order().orderId()).isEqualTo("BIG");
            assertThat(unassigned.reason()).isEqualTo(UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY);
        });
    }
}