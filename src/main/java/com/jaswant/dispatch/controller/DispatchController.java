package com.jaswant.dispatch.controller;

import com.jaswant.dispatch.dto.DispatchPlanDto;
import com.jaswant.dispatch.dto.MessageResponse;
import com.jaswant.dispatch.dto.OrdersRequest;
import com.jaswant.dispatch.dto.VehiclesRequest;
import com.jaswant.dispatch.service.DispatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    private final DispatchService service;

    public DispatchController(DispatchService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public MessageResponse acceptOrders(@Valid @RequestBody OrdersRequest request) {
        service.acceptOrders(request.orders());
        return MessageResponse.success("Delivery orders accepted.");
    }

    @PostMapping("/vehicles")
    public MessageResponse acceptVehicles(@Valid @RequestBody VehiclesRequest request) {
        service.acceptVehicles(request.vehicles());
        return MessageResponse.success("Vehicle details accepted.");
    }

    @GetMapping("/plan")
    public DispatchPlanDto plan() {
        return service.buildPlan();
    }
}