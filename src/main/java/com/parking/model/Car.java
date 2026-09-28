package com.parking.model;

/**
 * Concrete Vehicle implementation representing a Car.
 * Demonstrates Inheritance.
 */
public class Car extends Vehicle {

    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }

    @Override
    public double getRateMultiplier() {
        return 1.0;
    }
}
