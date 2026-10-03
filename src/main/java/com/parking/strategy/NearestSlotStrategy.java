package com.parking.strategy;

import com.parking.model.ParkingSlot;
import com.parking.model.SlotType;
import com.parking.model.Vehicle;
import com.parking.model.VehicleType;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds the nearest available compatible slot.
 * Prioritizes exact slot type match (e.g. BIKE slot for BIKE), then larger slots.
 */
public class NearestSlotStrategy implements SlotAllocationStrategy {

    @Override
    public Optional<ParkingSlot> findSlot(List<ParkingSlot> slots, Vehicle vehicle) {
        if (slots == null || vehicle == null) {
            return Optional.empty();
        }

        // Filter vacant and compatible slots
        List<ParkingSlot> availableSlots = slots.stream()
                .filter(slot -> !slot.isOccupied())
                .filter(slot -> slot.isCompatible(vehicle.getType()))
                .toList();

        if (availableSlots.isEmpty()) {
            return Optional.empty();
        }

        // Target ideal slot type for vehicle
        SlotType idealSlotType = getIdealSlotType(vehicle.getType());

        // Return first exact match, or fallback to first available compatible slot (by slot ID order)
        return availableSlots.stream()
                .sorted(Comparator.comparing((ParkingSlot slot) -> slot.getSlotType() == idealSlotType ? 0 : 1)
                        .thenComparing(ParkingSlot::getSlotId))
                .findFirst();
    }

    private SlotType getIdealSlotType(VehicleType vehicleType) {
        return switch (vehicleType) {
            case BIKE -> SlotType.BIKE;
            case CAR -> SlotType.COMPACT;
            case TRUCK -> SlotType.LARGE;
        };
    }
}
