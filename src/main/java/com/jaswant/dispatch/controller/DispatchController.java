package com.jaswant.dispatch.controller;

import com.jaswant.dispatch.dto.DispatchPlanDto;
import com.jaswant.dispatch.dto.ErrorResponse;
import com.jaswant.dispatch.dto.MessageResponse;
import com.jaswant.dispatch.dto.OrdersRequest;
import com.jaswant.dispatch.dto.VehiclesRequest;
import com.jaswant.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatch")
@Tag(name = "Dispatch", description = "Submit orders and vehicles, then fetch an optimized dispatch plan")
public class DispatchController {

    private final DispatchService service;

    public DispatchController(DispatchService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    @Operation(summary = "Submit delivery orders",
            description = "Upserts by orderId: new IDs are inserted and existing IDs are updated. "
                    + "The request is all-or-nothing.")
    @ApiResponse(responseCode = "400", description = "Invalid input or duplicate order IDs",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse acceptOrders(@Valid @RequestBody OrdersRequest request) {
        service.acceptOrders(request.orders());
        return MessageResponse.success("Delivery orders accepted.");
    }

    @PostMapping("/vehicles")
    @Operation(summary = "Submit fleet details",
            description = "Upserts by vehicleId: new IDs are inserted and existing IDs are updated. "
                    + "The request is all-or-nothing.")
    @ApiResponse(responseCode = "400", description = "Invalid input or duplicate vehicle IDs",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse acceptVehicles(@Valid @RequestBody VehiclesRequest request) {
        service.acceptVehicles(request.vehicles());
        return MessageResponse.success("Vehicle details accepted.");
    }

    @GetMapping("/plan")
    @Operation(summary = "Get the dispatch plan",
            description = "Assigns all stored orders to stored vehicles. HIGH-priority orders get capacity first, "
                    + "and each stop is placed where it adds the least distance. Orders that cannot fit are "
                    + "returned in unassignedOrders with a reason.")
    @ApiResponse(responseCode = "409", description = "No vehicles have been registered yet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DispatchPlanDto plan() {
        return service.buildPlan();
    }
}