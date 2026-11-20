package com.jaswant.dispatch.exception;

public class NoVehiclesException extends RuntimeException {

    public NoVehiclesException() {
        super("No vehicles registered. POST /api/dispatch/vehicles first.");
    }
}