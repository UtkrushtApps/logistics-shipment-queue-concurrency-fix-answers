package com.example.logistics.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable representation of a shipment.
 */
public final class Shipment {

    private final String id;
    private final String destination;
    private final double weightKg;
    private final Instant createdAt;

    public Shipment(String id, String destination, double weightKg) {
        this(id, destination, weightKg, Instant.now());
    }

    public Shipment(String id, String destination, double weightKg, Instant createdAt) {
        this.id = id;
        this.destination = destination;
        this.weightKg = weightKg;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getId() {
        return id;
    }

    public String getDestination() {
        return destination;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;
        return Double.compare(shipment.weightKg, weightKg) == 0
                && Objects.equals(id, shipment.id)
                && Objects.equals(destination, shipment.destination)
                && Objects.equals(createdAt, shipment.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, destination, weightKg, createdAt);
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "id='" + id + '\'' +
                ", destination='" + destination + '\'' +
                ", weightKg=" + weightKg +
                ", createdAt=" + createdAt +
                '}';
    }
}
