package com.jaswant.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrdersRequest(@NotEmpty List<@NotNull @Valid OrderDto> orders) {
}