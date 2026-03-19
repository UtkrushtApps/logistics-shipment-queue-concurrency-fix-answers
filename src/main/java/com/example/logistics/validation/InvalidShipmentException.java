package com.example.logistics.validation;

/**
 * Thrown when a shipment fails validation.
 */
public class InvalidShipmentException extends RuntimeException {

    public InvalidShipmentException(String message) {
        super(message);
    }
}
