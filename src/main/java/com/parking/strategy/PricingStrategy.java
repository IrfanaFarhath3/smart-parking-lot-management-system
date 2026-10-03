package com.parking.strategy;

import com.parking.model.Ticket;
import com.parking.model.Vehicle;

import java.time.LocalDateTime;

/**
 * Strategy interface for calculating parking fees.
 */
public interface PricingStrategy {
    /**
     * Calculates fee based on ticket details, vehicle properties, and exit time.
     *
     * @param ticket The parking ticket
     * @param vehicle The parked vehicle (provides rate multiplier)
     * @param exitTime Time of exit
     * @return Calculated fee amount
     */
    double calculateFee(Ticket ticket, Vehicle vehicle, LocalDateTime exitTime);
}
