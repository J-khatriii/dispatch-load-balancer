package com.jaswant.dispatch.dto;

import com.jaswant.dispatch.domain.Priority;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderDto(
        @NotBlank String orderId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank String address,
        @NotNull @Positive Double packageWeight,
        @NotNull Priority priority
) {
}