package com.jaswant.dispatch.dto;

import jakarta.validation.constraints.*;

public record VehicleDto(
        @NotBlank @Size(max = 64) String vehicleId,
        @NotNull @Positive Double capacity,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double currentLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double currentLongitude,
        @NotBlank String currentAddress
) {
}