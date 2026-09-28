package com.parking.model;

/**
 * Concrete Vehicle implementation representing a Truck.
 * Demonstrates Inheritance.
 */
public class Truck extends Vehicle {

    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }

    @Override
    public double getRateMultiplier() {
        return 2.0;
    }
}
