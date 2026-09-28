package com.parking.model;

import java.util.Objects;

/**
 * Abstract base class representing a Vehicle in the parking system.
 * Demonstrates Abstraction and Encapsulation.
 */
public abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    /**
     * Constructs a Vehicle with the specified license plate and vehicle type.
     *
     * @param licensePlate Unique license plate number
     * @param type         Type of the vehicle
     */
    public Vehicle(String licensePlate, VehicleType type) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            throw new IllegalArgumentException("License plate cannot be null or empty.");
        }
        this.licensePlate = licensePlate.trim().toUpperCase();
        this.type = Objects.requireNonNull(type, "Vehicle type cannot be null.");
    }

    /**
     * @return The uppercase license plate number of the vehicle.
     */
    public String getLicensePlate() {
        return licensePlate;
    }

    /**
     * @return The vehicle type.
     */
    public VehicleType getType() {
        return type;
    }

    /**
     * Polymorphic method to get rate multiplier for fee calculation.
     *
     * @return Rate multiplier (e.g. 0.5 for Bike, 1.0 for Car, 2.0 for Truck)
     */
    public abstract double getRateMultiplier();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle vehicle)) return false;
        return Objects.equals(licensePlate, vehicle.licensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licensePlate);
    }

    @Override
    public String toString() {
        return type + " [" + licensePlate + "]";
    }
}
