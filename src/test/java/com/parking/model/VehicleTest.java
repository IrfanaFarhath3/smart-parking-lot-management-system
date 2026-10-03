package com.parking.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    @Test
    @DisplayName("Test Bike properties and rate multiplier")
    void testBikeCreation() {
        Vehicle bike = new Bike("bk-123");
        assertEquals("BK-123", bike.getLicensePlate());
        assertEquals(VehicleType.BIKE, bike.getType());
        assertEquals(0.5, bike.getRateMultiplier());
    }

    @Test
    @DisplayName("Test Car properties and rate multiplier")
    void testCarCreation() {
        Vehicle car = new Car("cr-456");
        assertEquals("CR-456", car.getLicensePlate());
        assertEquals(VehicleType.CAR, car.getType());
        assertEquals(1.0, car.getRateMultiplier());
    }

    @Test
    @DisplayName("Test Truck properties and rate multiplier")
    void testTruckCreation() {
        Vehicle truck = new Truck("tr-789");
        assertEquals("TR-789", truck.getLicensePlate());
        assertEquals(VehicleType.TRUCK, truck.getType());
        assertEquals(2.0, truck.getRateMultiplier());
    }

    @Test
    @DisplayName("Test null or empty license plate exception")
    void testInvalidLicensePlate() {
        assertThrows(IllegalArgumentException.class, () -> new Car(null));
        assertThrows(IllegalArgumentException.class, () -> new Car("   "));
    }
}
