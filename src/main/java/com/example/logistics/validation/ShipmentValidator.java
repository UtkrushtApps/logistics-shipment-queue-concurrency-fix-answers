package com.example.logistics.validation;

import com.example.logistics.model.Shipment;

import java.util.logging.Logger;

/**
 * Performs basic validation of incoming shipments.
 */
public class ShipmentValidator {

    private static final Logger LOGGER = Logger.getLogger(ShipmentValidator.class.getName());

    /**
     * Validate a shipment. Throws {@link InvalidShipmentException} when invalid.
     */
    public void validate(Shipment shipment) {
        if (shipment == null) {
            throw new InvalidShipmentException("Shipment must not be null");
        }

        if (shipment.getId() == null || shipment.getId().trim().isEmpty()) {
            throw new InvalidShipmentException("Shipment id must not be null or blank");
        }

        if (shipment.getDestination() == null || shipment.getDestination().trim().isEmpty()) {
            throw new InvalidShipmentException("Destination must not be null or blank");
        }

        if (shipment.getWeightKg() <= 0.0) {
            throw new InvalidShipmentException("Weight must be positive");
        }

        // Extra observability hook (at fine level to avoid noise under load)
        if (LOGGER.isLoggable(java.util.logging.Level.FINER)) {
            LOGGER.finer(() -> "Validated shipment " + shipment.getId());
        }
    }
}
