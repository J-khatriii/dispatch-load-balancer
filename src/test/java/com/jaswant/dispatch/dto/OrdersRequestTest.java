package com.jaswant.dispatch.dto;

import com.jaswant.dispatch.domain.Priority;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class OrdersRequestTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static OrderDto order(String id, Double latitude) {
        return new OrderDto(id, latitude, 77.5946, "MG Road, Bangalore", 10.0, Priority.HIGH);
    }

    private static Set<String> invalidPaths(OrdersRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(toSet());
    }

    @Test
    void validRequestHasNoViolations() {
        assertThat(invalidPaths(new OrdersRequest(List.of(order("ORD001", 12.9716))))).isEmpty();
    }

    @Test
    void missingListIsRejected() {
        assertThat(invalidPaths(new OrdersRequest(null))).containsExactly("orders");
    }

    @Test
    void emptyListIsRejected() {
        assertThat(invalidPaths(new OrdersRequest(List.of()))).containsExactly("orders");
    }

    @Test
    void invalidOrderIsReportedWithItsIndex() {
        var request = new OrdersRequest(List.of(order("ORD001", 12.9716), order("ORD002", 123.0)));

        assertThat(invalidPaths(request)).containsExactly("orders[1].latitude");
    }

    @Test
    void nullElementIsRejected() {
        var request = new OrdersRequest(Arrays.asList(order("ORD001", 12.9716), null));

        assertThat(validator.validate(request)).hasSize(1);
    }
}