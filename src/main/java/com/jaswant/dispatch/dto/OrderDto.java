package com.jaswant.dispatch.dto;

import com.jaswant.dispatch.domain.Priority;
import jakarta.validation.constraints.*;

public record OrderDto(
        @NotBlank @Size(max = 64) String orderId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank String address,
        @NotNull @Positive Double packageWeight,
        @NotNull Priority priority
) {
}