package com.parking.strategy;

import com.parking.model.Ticket;
import com.parking.model.Vehicle;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Hourly pricing strategy. Calculates fees using base hourly rate * vehicle rate multiplier * duration in hours.
 * Partial hours are rounded up to full hours (minimum 1 hour).
 */
public class HourlyPricingStrategy implements PricingStrategy {
    private final double baseHourlyRate;

    public HourlyPricingStrategy() {
        this(10.0); // Default $10.0 per hour
    }

    public HourlyPricingStrategy(double baseHourlyRate) {
        if (baseHourlyRate < 0) {
            throw new IllegalArgumentException("Base hourly rate cannot be negative.");
        }
        this.baseHourlyRate = baseHourlyRate;
    }

    public double getBaseHourlyRate() {
        return baseHourlyRate;
    }

    @Override
    public double calculateFee(Ticket ticket, Vehicle vehicle, LocalDateTime exitTime) {
        if (ticket == null || exitTime == null) {
            throw new IllegalArgumentException("Ticket and exit time cannot be null.");
        }
        if (exitTime.isBefore(ticket.getEntryTime())) {
            throw new IllegalArgumentException("Exit time cannot be before entry time.");
        }

        Duration duration = Duration.between(ticket.getEntryTime(), exitTime);
        long minutes = duration.toMinutes();

        // Round up to full hours, minimum 1 hour
        long hours = (long) Math.ceil(minutes / 60.0);
        if (hours == 0) {
            hours = 1;
        }

        double multiplier = (vehicle != null) ? vehicle.getRateMultiplier() : 1.0;
        return hours * baseHourlyRate * multiplier;
    }
}
