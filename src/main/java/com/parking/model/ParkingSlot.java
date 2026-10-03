package com.parking.model;

import java.util.Objects;

/**
 * Represents a individual parking slot in the parking lot.
 */
public class ParkingSlot {
    private final String slotId;
    private final SlotType slotType;
    private boolean occupied;
    private Vehicle parkedVehicle;

    public ParkingSlot(String slotId, SlotType slotType) {
        if (slotId == null || slotId.trim().isEmpty()) {
            throw new IllegalArgumentException("Slot ID cannot be null or empty.");
        }
        this.slotId = slotId.trim().toUpperCase();
        this.slotType = Objects.requireNonNull(slotType, "Slot type cannot be null.");
        this.occupied = false;
        this.parkedVehicle = null;
    }

    public ParkingSlot(String slotId, SlotType slotType, boolean occupied, Vehicle parkedVehicle) {
        this.slotId = slotId;
        this.slotType = slotType;
        this.occupied = occupied;
        this.parkedVehicle = parkedVehicle;
    }

    public String getSlotId() {
        return slotId;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public Vehicle getParkedVehicle() {
        return parkedVehicle;
    }

    public void park(Vehicle vehicle) {
        if (this.occupied) {
            throw new IllegalStateException("Slot " + slotId + " is already occupied.");
        }
        if (!isCompatible(vehicle.getType())) {
            throw new IllegalArgumentException("Vehicle type " + vehicle.getType() + " is incompatible with slot type " + slotType);
        }
        this.parkedVehicle = vehicle;
        this.occupied = true;
    }

    public void vacate() {
        if (!this.occupied) {
            throw new IllegalStateException("Slot " + slotId + " is already vacant.");
        }
        this.parkedVehicle = null;
        this.occupied = false;
    }

    /**
     * Checks if a vehicle type can fit into this slot type.
     * Bike -> BIKE, COMPACT, LARGE
     * Car -> COMPACT, LARGE
     * Truck -> LARGE
     */
    public boolean isCompatible(VehicleType vehicleType) {
        return switch (vehicleType) {
            case BIKE -> true; // Bike fits in any slot
            case CAR -> slotType == SlotType.COMPACT || slotType == SlotType.LARGE;
            case TRUCK -> slotType == SlotType.LARGE;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParkingSlot that)) return false;
        return Objects.equals(slotId, that.slotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId);
    }

    @Override
    public String toString() {
        return "Slot[" + slotId + ", Type=" + slotType + ", Occupied=" + occupied + 
               (parkedVehicle != null ? ", Vehicle=" + parkedVehicle.getLicensePlate() : "") + "]";
    }
}
