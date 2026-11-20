package com.jaswant.dispatch.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class VehicleDtoTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static VehicleDto vehicle(String id, Double capacity, Double lat, Double lon, String address) {
        return new VehicleDto(id, capacity, lat, lon, address);
    }

    private static Set<String> invalidFields(VehicleDto dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(toSet());
    }

    @Test
    void validVehicleHasNoViolations() {
        var dto = vehicle("VEH001", 100.0, 12.9716, 77.6413, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto)).isEmpty();
    }

    @Test
    void blankVehicleIdIsRejected() {
        var dto = vehicle(" ", 100.0, 12.9716, 77.6413, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto)).containsExactly("vehicleId");
    }

    @Test
    void blankAddressIsRejected() {
        var dto = vehicle("VEH001", 100.0, 12.9716, 77.6413, "");
        assertThat(invalidFields(dto)).containsExactly("currentAddress");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -10})
    void nonPositiveCapacityIsRejected(double capacity) {
        var dto = vehicle("VEH001", capacity, 12.9716, 77.6413, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto)).containsExactly("capacity");
    }

    @Test
    void missingFieldsAreRejected() {
        var dto = vehicle("VEH001", null, null, null, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto))
                .containsExactlyInAnyOrder("capacity", "currentLatitude", "currentLongitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.1, 90.1})
    void latitudeOutOfRangeIsRejected(double latitude) {
        var dto = vehicle("VEH001", 100.0, latitude, 77.6413, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto)).containsExactly("currentLatitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.1, 180.1})
    void longitudeOutOfRangeIsRejected(double longitude) {
        var dto = vehicle("VEH001", 100.0, 12.9716, longitude, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto)).containsExactly("currentLongitude");
    }

    @Test
    void vehicleIdLongerThan64CharactersIsRejected() {
        var dto = vehicle("X".repeat(65), 100.0, 12.9716, 77.6413, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto)).containsExactly("vehicleId");
    }

    @Test
    void vehicleIdOfExactly64CharactersIsAccepted() {
        var dto = vehicle("X".repeat(64), 100.0, 12.9716, 77.6413, "Indiranagar, Bangalore");
        assertThat(invalidFields(dto)).isEmpty();
    }
}