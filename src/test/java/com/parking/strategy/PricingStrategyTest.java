package com.parking.strategy;

import com.parking.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PricingStrategyTest {

    private PricingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new HourlyPricingStrategy(10.0); // $10/hour base rate
    }

    @Test
    @DisplayName("Test 1 hour fee calculation for Car")
    void testOneHourCarFee() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 3, 10, 0);
        LocalDateTime exit = LocalDateTime.of(2026, 10, 3, 11, 0);
        Ticket ticket = new Ticket("T1", "CAR-100", VehicleType.CAR, "C-01", entry, null, 0.0, false);
        Vehicle car = new Car("CAR-100");

        double fee = strategy.calculateFee(ticket, car, exit);
        assertEquals(10.0, fee, 0.001);
    }

    @Test
    @DisplayName("Test fractional hour rounds up (1 hour 15 mins -> 2 hours)")
    void testFractionalHourRounding() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 3, 10, 0);
        LocalDateTime exit = LocalDateTime.of(2026, 10, 3, 11, 15);
        Ticket ticket = new Ticket("T2", "BIKE-200", VehicleType.BIKE, "B-01", entry, null, 0.0, false);
        Vehicle bike = new Bike("BIKE-200"); // Multiplier 0.5

        // 2 hours * $10 * 0.5 multiplier = $10.0
        double fee = strategy.calculateFee(ticket, bike, exit);
        assertEquals(10.0, fee, 0.001);
    }

    @Test
    @DisplayName("Test 3 hours fee for Truck")
    void testTruckFee() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 3, 10, 0);
        LocalDateTime exit = LocalDateTime.of(2026, 10, 3, 13, 0);
        Ticket ticket = new Ticket("T3", "TRUCK-300", VehicleType.TRUCK, "L-01", entry, null, 0.0, false);
        Vehicle truck = new Truck("TRUCK-300"); // Multiplier 2.0

        // 3 hours * $10 * 2.0 multiplier = $60.0
        double fee = strategy.calculateFee(ticket, truck, exit);
        assertEquals(60.0, fee, 0.001);
    }
}
