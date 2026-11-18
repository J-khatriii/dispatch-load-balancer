package com.jaswant.dispatch.dto;

import com.jaswant.dispatch.domain.Priority;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class OrderDtoTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static OrderDto order(String id, Double lat, Double lon, String address, Double weight, Priority priority) {
        return new OrderDto(id, lat, lon, address, weight, priority);
    }

    private static OrderDto validOrder() {
        return order("ORD001", 12.9716, 77.5946, "MG Road, Bangalore", 10.0, Priority.HIGH);
    }

    private static Set<String> invalidFields(OrderDto dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(toSet());
    }

    @Test
    void validOrderHasNoViolations() {
        assertThat(invalidFields(validOrder())).isEmpty();
    }

    @Test
    void blankOrderIdIsRejected() {
        var dto = order(" ", 12.9716, 77.5946, "MG Road, Bangalore", 10.0, Priority.HIGH);
        assertThat(invalidFields(dto)).containsExactly("orderId");
    }

    @Test
    void blankAddressIsRejected() {
        var dto = order("ORD001", 12.9716, 77.5946, "", 10.0, Priority.HIGH);
        assertThat(invalidFields(dto)).containsExactly("address");
    }

    @Test
    void missingCoordinatesAreRejected() {
        var dto = order("ORD001", null, null, "MG Road, Bangalore", 10.0, Priority.HIGH);
        assertThat(invalidFields(dto)).containsExactlyInAnyOrder("latitude", "longitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.1, 90.1})
    void latitudeOutOfRangeIsRejected(double latitude) {
        var dto = order("ORD001", latitude, 77.5946, "MG Road, Bangalore", 10.0, Priority.HIGH);
        assertThat(invalidFields(dto)).containsExactly("latitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.1, 180.1})
    void longitudeOutOfRangeIsRejected(double longitude) {
        var dto = order("ORD001", 12.9716, longitude, "MG Road, Bangalore", 10.0, Priority.HIGH);
        assertThat(invalidFields(dto)).containsExactly("longitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -5})
    void nonPositiveWeightIsRejected(double weight) {
        var dto = order("ORD001", 12.9716, 77.5946, "MG Road, Bangalore", weight, Priority.HIGH);
        assertThat(invalidFields(dto)).containsExactly("packageWeight");
    }

    @Test
    void missingPriorityIsRejected() {
        var dto = order("ORD001", 12.9716, 77.5946, "MG Road, Bangalore", 10.0, null);
        assertThat(invalidFields(dto)).containsExactly("priority");
    }
}