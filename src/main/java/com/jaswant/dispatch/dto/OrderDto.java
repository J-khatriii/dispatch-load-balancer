package com.jaswant.dispatch.dto;

import com.jaswant.dispatch.domain.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OrderDto(
        @NotBlank @Size(max = 64) @Schema(example = "ORD001") String orderId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Schema(example = "12.9716") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Schema(example = "77.5946") Double longitude,
        @NotBlank @Schema(example = "MG Road, Bangalore, Karnataka, India") String address,
        @NotNull @Positive @Schema(example = "10") Double packageWeight,
        @NotNull Priority priority
) {
}