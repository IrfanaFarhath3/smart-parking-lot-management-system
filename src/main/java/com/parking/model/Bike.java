package com.parking.model;

/**
 * Concrete Vehicle implementation representing a Bike.
 * Demonstrates Inheritance.
 */
public class Bike extends Vehicle {

    public Bike(String licensePlate) {
        super(licensePlate, VehicleType.BIKE);
    }

    @Override
    public double getRateMultiplier() {
        return 0.5;
    }
}
