package com.jaswant.dispatch.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class VehiclesRequestTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static VehicleDto vehicle(String id, Double capacity) {
        return new VehicleDto(id, capacity, 12.9716, 77.6413, "Indiranagar, Bangalore");
    }

    private static Set<String> invalidPaths(VehiclesRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(toSet());
    }

    @Test
    void validRequestHasNoViolations() {
        assertThat(invalidPaths(new VehiclesRequest(List.of(vehicle("VEH001", 100.0))))).isEmpty();
    }

    @Test
    void missingListIsRejected() {
        assertThat(invalidPaths(new VehiclesRequest(null))).containsExactly("vehicles");
    }

    @Test
    void emptyListIsRejected() {
        assertThat(invalidPaths(new VehiclesRequest(List.of()))).containsExactly("vehicles");
    }

    @Test
    void invalidVehicleIsReportedWithItsIndex() {
        var request = new VehiclesRequest(List.of(vehicle("VEH001", 100.0), vehicle("VEH002", -5.0)));

        assertThat(invalidPaths(request)).containsExactly("vehicles[1].capacity");
    }

    @Test
    void nullElementIsRejected() {
        var request = new VehiclesRequest(Arrays.asList(vehicle("VEH001", 100.0), null));

        assertThat(validator.validate(request)).hasSize(1);
    }
}