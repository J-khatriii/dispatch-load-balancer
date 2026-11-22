package com.jaswant.dispatch.integration;

import com.jaswant.dispatch.TestcontainersConfiguration;
import com.jaswant.dispatch.domain.Priority;
import com.jaswant.dispatch.dto.OrderDto;
import com.jaswant.dispatch.repository.OrderRepository;
import com.jaswant.dispatch.service.DispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DispatchIntegrationTest {

    private static final String ORDERS_URI = "/api/dispatch/orders";
    private static final String VEHICLES_URI = "/api/dispatch/vehicles";
    private static final String PLAN_URI = "/api/dispatch/plan";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DispatchService service;

    @BeforeEach
    void cleanTables() {
        jdbc.execute("TRUNCATE TABLE orders, vehicles");
    }

    private static String orderJson(String id, double longitude, double weight, String priority) {
        return """
                {"orderId": "%s", "latitude": 0, "longitude": %s, "address": "Address %s",
                 "packageWeight": %s, "priority": "%s"}"""
                .formatted(id, longitude, id, weight, priority);
    }

    private static String vehicleJson(String id, double capacity, double longitude) {
        return """
                {"vehicleId": "%s", "capacity": %s, "currentLatitude": 0, "currentLongitude": %s,
                 "currentAddress": "Depot %s"}"""
                .formatted(id, capacity, longitude, id);
    }

    private static String wrap(String key, String... items) {
        return "{\"%s\": [%s]}".formatted(key, String.join(",", items));
    }

    private MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private void postAccepted(String uri, String body) {
        assertThat(post(uri, body)).hasStatusOk();
    }

    @Test
    void fullFlowProducesTheExpectedPlan() {
        postAccepted(VEHICLES_URI, wrap("vehicles", vehicleJson("V1", 100, 0), vehicleJson("V2", 100, 10)));
        postAccepted(ORDERS_URI, wrap("orders",
                orderJson("A", 1, 40, "HIGH"),
                orderJson("B", 9, 40, "MEDIUM"),
                orderJson("BIG", 5, 500, "LOW")));

        var plan = assertThat(mvc.get().uri(PLAN_URI).exchange());

        plan.hasStatusOk();
        plan.bodyJson().extractingPath("$.dispatchPlan[0].vehicleId").isEqualTo("V1");
        plan.bodyJson().extractingPath("$.dispatchPlan[0].assignedOrders[*].orderId").asList().containsExactly("A");
        plan.bodyJson().extractingPath("$.dispatchPlan[0].totalLoad").isEqualTo(40.0);
        plan.bodyJson().extractingPath("$.dispatchPlan[0].totalDistance").isEqualTo("111.19 km");
        plan.bodyJson().extractingPath("$.dispatchPlan[1].vehicleId").isEqualTo("V2");
        plan.bodyJson().extractingPath("$.dispatchPlan[1].assignedOrders[*].orderId").asList().containsExactly("B");
        plan.bodyJson().extractingPath("$.dispatchPlan[1].totalDistance").isEqualTo("111.19 km");
        plan.bodyJson().extractingPath("$.unassignedOrders[0].order.orderId").isEqualTo("BIG");
        plan.bodyJson().extractingPath("$.unassignedOrders[0].reason").isEqualTo("EXCEEDS_MAX_VEHICLE_CAPACITY");
    }

    @Test
    void resendingAnOrderUpdatesItInPlace() {
        postAccepted(ORDERS_URI, wrap("orders", orderJson("A", 1, 10, "HIGH")));
        postAccepted(ORDERS_URI, wrap("orders", orderJson("A", 1, 20, "LOW")));

        assertThat(orderRepository.findAll()).singleElement().satisfies(order -> {
            assertThat(order.weight()).isEqualTo(20);
            assertThat(order.priority()).isEqualTo(Priority.LOW);
        });
    }

    @Test
    void planWithoutVehiclesIsAConflict() {
        postAccepted(ORDERS_URI, wrap("orders", orderJson("A", 1, 10, "HIGH")));

        assertThat(mvc.get().uri(PLAN_URI).exchange()).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void planWithoutOrdersKeepsVehiclesIdle() {
        postAccepted(VEHICLES_URI, wrap("vehicles", vehicleJson("V1", 100, 0)));

        var plan = assertThat(mvc.get().uri(PLAN_URI).exchange());

        plan.hasStatusOk();
        plan.bodyJson().extractingPath("$.dispatchPlan[0].assignedOrders").asList().isEmpty();
        plan.bodyJson().extractingPath("$.unassignedOrders").asList().isEmpty();
    }

    @Test
    void duplicateIdsInOneRequestStoreNothing() {
        var result = assertThat(post(ORDERS_URI,
                wrap("orders", orderJson("A", 1, 10, "HIGH"), orderJson("A", 2, 20, "LOW"))));

        result.hasStatus(HttpStatus.BAD_REQUEST);
        result.bodyJson().extractingPath("$.message").asString().contains("A");
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    void failedBatchLeavesNothingBehind() {
        // Postgres rejects NUL characters that validation lets through, so the second batch of 1,000 fails.
        List<OrderDto> orders = IntStream.range(0, 1_500)
                .mapToObj(i -> new OrderDto("ORD%05d".formatted(i), 12.0, 77.0,
                        i == 1_499 ? "bad\0address" : "Address", 1.0, Priority.MEDIUM))
                .toList();

        assertThatThrownBy(() -> service.acceptOrders(orders)).isInstanceOf(DataAccessException.class);

        assertThat(orderRepository.findAll()).isEmpty();
    }
}