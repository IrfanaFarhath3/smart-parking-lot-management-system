package com.parking.strategy;

import com.parking.model.ParkingSlot;
import com.parking.model.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for selecting an optimal available slot for a vehicle.
 */
public interface SlotAllocationStrategy {
    /**
     * Finds an available compatible parking slot for the vehicle.
     *
     * @param slots List of all available slots in the parking lot
     * @param vehicle The vehicle requesting a slot
     * @return Optional containing the selected ParkingSlot, or empty if no slot is available
     */
    Optional<ParkingSlot> findSlot(List<ParkingSlot> slots, Vehicle vehicle);
}
