package com.jaswant.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VehiclesRequest(@NotEmpty List<@NotNull @Valid VehicleDto> vehicles) {
}