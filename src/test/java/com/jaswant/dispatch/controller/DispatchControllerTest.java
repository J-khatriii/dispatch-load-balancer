package com.jaswant.dispatch.controller;

import com.jaswant.dispatch.domain.Priority;
import com.jaswant.dispatch.dto.DispatchPlanDto;
import com.jaswant.dispatch.dto.OrderDto;
import com.jaswant.dispatch.dto.VehicleDto;
import com.jaswant.dispatch.dto.VehiclePlanDto;
import com.jaswant.dispatch.exception.InvalidRequestException;
import com.jaswant.dispatch.exception.NoVehiclesException;
import com.jaswant.dispatch.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(DispatchController.class)
class DispatchControllerTest {

    private static final String ORDERS_URI = "/api/dispatch/orders";
    private static final String VEHICLES_URI = "/api/dispatch/vehicles";
    private static final String PLAN_URI = "/api/dispatch/plan";

    private static final String ORDERS_JSON = """
            {"orders": [{"orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
            "address": "MG Road, Bangalore", "packageWeight": 10, "priority": "HIGH"}]}
            """;

    private static final String VEHICLES_JSON = """
            {"vehicles": [{"vehicleId": "VEH001", "capacity": 100, "currentLatitude": 12.9716,
            "currentLongitude": 77.6413, "currentAddress": "Indiranagar, Bangalore"}]}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DispatchService service;

    private MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    void validOrdersAreAcceptedAndPassedToTheService() {
        assertThat(post(ORDERS_URI, ORDERS_JSON)).hasStatusOk().bodyJson()
                .extractingPath("$.message").isEqualTo("Delivery orders accepted.");

        verify(service).acceptOrders(List.of(
                new OrderDto("ORD001", 12.9716, 77.5946, "MG Road, Bangalore", 10.0, Priority.HIGH)));
    }

    @Test
    void successResponseCarriesSuccessStatus() {
        assertThat(post(ORDERS_URI, ORDERS_JSON)).bodyJson().extractingPath("$.status").isEqualTo("success");
    }

    @Test
    void invalidOrderIsRejectedWithFieldLevelDetails() {
        String body = ORDERS_JSON.replace("12.9716", "123.0");

        var result = assertThat(post(ORDERS_URI, body));

        result.hasStatus(HttpStatus.BAD_REQUEST);
        result.bodyJson().extractingPath("$.status").isEqualTo("error");
        result.bodyJson().extractingPath("$.details[0]").asString().startsWith("orders[0].latitude");
        verifyNoInteractions(service);
    }

    @Test
    void emptyOrderListIsRejected() {
        assertThat(post(ORDERS_URI, "{\"orders\": []}")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonIsRejectedWithAClearError() {
        var result = assertThat(post(ORDERS_URI, "{\"orders\": [ {"));

        result.hasStatus(HttpStatus.BAD_REQUEST);
        result.bodyJson().extractingPath("$.status").isEqualTo("error");
        verifyNoInteractions(service);
    }

    @Test
    void unknownPriorityIsRejected() {
        String body = ORDERS_JSON.replace("HIGH", "URGENT");

        assertThat(post(ORDERS_URI, body)).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(service);
    }

    @Test
    void duplicateIdsFromTheServiceBecomeABadRequest() {
        doThrow(new InvalidRequestException("Duplicate order IDs in request: ORD001"))
                .when(service).acceptOrders(anyList());

        var result = assertThat(post(ORDERS_URI, ORDERS_JSON));

        result.hasStatus(HttpStatus.BAD_REQUEST);
        result.bodyJson().extractingPath("$.message").isEqualTo("Duplicate order IDs in request: ORD001");
    }

    @Test
    void validVehiclesAreAcceptedAndPassedToTheService() {
        assertThat(post(VEHICLES_URI, VEHICLES_JSON)).hasStatusOk().bodyJson()
                .extractingPath("$.message").isEqualTo("Vehicle details accepted.");

        verify(service).acceptVehicles(List.of(
                new VehicleDto("VEH001", 100.0, 12.9716, 77.6413, "Indiranagar, Bangalore")));
    }

    @Test
    void invalidVehicleIsRejectedWithFieldLevelDetails() {
        String body = VEHICLES_JSON.replace("\"capacity\": 100", "\"capacity\": -5");

        var result = assertThat(post(VEHICLES_URI, body));

        result.hasStatus(HttpStatus.BAD_REQUEST);
        result.bodyJson().extractingPath("$.details[0]").asString().startsWith("vehicles[0].capacity");
        verifyNoInteractions(service);
    }

    @Test
    void planIsReturnedInTheSpecifiedShape() {
        var order = new OrderDto("ORD001", 12.9716, 77.5946, "MG Road, Bangalore", 10.0, Priority.HIGH);
        var vehiclePlan = new VehiclePlanDto("VEH001", "Depot", 100, 10, "5.00 km", List.of(order));
        when(service.buildPlan()).thenReturn(new DispatchPlanDto(List.of(vehiclePlan), List.of()));

        var result = assertThat(mvc.get().uri(PLAN_URI).exchange());

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.dispatchPlan[0].vehicleId").isEqualTo("VEH001");
        result.bodyJson().extractingPath("$.dispatchPlan[0].totalDistance").isEqualTo("5.00 km");
        result.bodyJson().extractingPath("$.dispatchPlan[0].assignedOrders[0].orderId").isEqualTo("ORD001");
    }

    @Test
    void planWithoutVehiclesIsAConflict() {
        when(service.buildPlan()).thenThrow(new NoVehiclesException());

        var result = assertThat(mvc.get().uri(PLAN_URI).exchange());

        result.hasStatus(HttpStatus.CONFLICT);
        result.bodyJson().extractingPath("$.status").isEqualTo("error");
    }
}