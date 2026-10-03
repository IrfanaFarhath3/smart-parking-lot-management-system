package com.parking.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a parking ticket generated when a vehicle parks.
 */
public class Ticket {
    private final String ticketId;
    private final String licensePlate;
    private final VehicleType vehicleType;
    private final String slotId;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private double fee;
    private boolean paid;

    /**
     * Constructor for issuing a new ticket upon parking.
     */
    public Ticket(String licensePlate, VehicleType vehicleType, String slotId) {
        this("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             licensePlate, vehicleType, slotId, LocalDateTime.now(), null, 0.0, false);
    }

    /**
     * Constructor for reconstructing a ticket (e.g., from DB or with explicit timestamps).
     */
    public Ticket(String ticketId, String licensePlate, VehicleType vehicleType, 
                  String slotId, LocalDateTime entryTime, LocalDateTime exitTime, 
                  double fee, boolean paid) {
        this.ticketId = Objects.requireNonNull(ticketId, "Ticket ID cannot be null.");
        this.licensePlate = Objects.requireNonNull(licensePlate, "License plate cannot be null.").toUpperCase();
        this.vehicleType = Objects.requireNonNull(vehicleType, "Vehicle type cannot be null.");
        this.slotId = Objects.requireNonNull(slotId, "Slot ID cannot be null.");
        this.entryTime = Objects.requireNonNull(entryTime, "Entry time cannot be null.");
        this.exitTime = exitTime;
        this.fee = fee;
        this.paid = paid;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getSlotId() {
        return slotId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket ticket)) return false;
        return Objects.equals(ticketId, ticket.ticketId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId);
    }

    @Override
    public String toString() {
        return "Ticket[" + ticketId + ", License=" + licensePlate + 
               ", Slot=" + slotId + ", Entry=" + entryTime + 
               (exitTime != null ? ", Exit=" + exitTime : "") + 
               ", Fee=$" + String.format("%.2f", fee) + 
               ", Paid=" + paid + "]";
    }
}
