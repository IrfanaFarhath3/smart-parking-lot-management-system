package com.parking.service;

import com.parking.dao.ParkingSlotDAO;
import com.parking.dao.TicketDAO;
import com.parking.model.*;
import com.parking.strategy.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class handling core business operations for the Smart Parking Lot.
 */
public class ParkingLotService {
    private final ParkingSlotDAO slotDAO;
    private final TicketDAO ticketDAO;
    private final SlotAllocationStrategy allocationStrategy;
    private final PricingStrategy pricingStrategy;

    public ParkingLotService() {
        this(new ParkingSlotDAO(), new TicketDAO(), new NearestSlotStrategy(), new HourlyPricingStrategy());
    }

    public ParkingLotService(ParkingSlotDAO slotDAO, TicketDAO ticketDAO, 
                              SlotAllocationStrategy allocationStrategy, PricingStrategy pricingStrategy) {
        this.slotDAO = slotDAO;
        this.ticketDAO = ticketDAO;
        this.allocationStrategy = allocationStrategy;
        this.pricingStrategy = pricingStrategy;
    }

    /**
     * Initializes or resets the parking lot layout in the database.
     *
     * @param bikeSlots   Number of bike slots to create
     * @param compactSlots Number of compact vehicle slots to create
     * @param largeSlots   Number of large vehicle slots to create
     */
    public void setupParkingLot(int bikeSlots, int compactSlots, int largeSlots) {
        List<ParkingSlot> slots = new ArrayList<>();

        for (int i = 1; i <= bikeSlots; i++) {
            slots.add(new ParkingSlot(String.format("B-%02d", i), SlotType.BIKE));
        }
        for (int i = 1; i <= compactSlots; i++) {
            slots.add(new ParkingSlot(String.format("C-%02d", i), SlotType.COMPACT));
        }
        for (int i = 1; i <= largeSlots; i++) {
            slots.add(new ParkingSlot(String.format("L-%02d", i), SlotType.LARGE));
        }

        slotDAO.deleteAll();
        ticketDAO.deleteAll();
        slotDAO.saveAll(slots);
    }

    /**
     * Parks a vehicle in an available slot and generates a parking ticket.
     *
     * @param vehicle The vehicle to park
     * @return Issued Ticket
     */
    public Ticket parkVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null.");
        }

        // Check if vehicle is already actively parked
        Optional<Ticket> existingActiveTicket = ticketDAO.findActiveByLicensePlate(vehicle.getLicensePlate());
        if (existingActiveTicket.isPresent()) {
            throw new IllegalStateException("Vehicle with license plate " + vehicle.getLicensePlate() + 
                                           " is already parked in slot " + existingActiveTicket.get().getSlotId());
        }

        List<ParkingSlot> slots = slotDAO.findAll();
        Optional<ParkingSlot> targetSlot = allocationStrategy.findSlot(slots, vehicle);

        if (targetSlot.isEmpty()) {
            throw new IllegalStateException("No available slot found for vehicle type: " + vehicle.getType());
        }

        ParkingSlot slot = targetSlot.get();
        slot.park(vehicle);
        slotDAO.save(slot);

        Ticket ticket = new Ticket(vehicle.getLicensePlate(), vehicle.getType(), slot.getSlotId());
        ticketDAO.save(ticket);

        return ticket;
    }

    /**
     * Unparks a vehicle, calculates the fee, updates ticket and slot status.
     *
     * @param ticketId Ticket ID
     * @return Completed Ticket with fee breakdown
     */
    public Ticket unparkVehicle(String ticketId) {
        return unparkVehicle(ticketId, LocalDateTime.now());
    }

    /**
     * Unparks a vehicle with a custom exit timestamp (useful for simulation or testing).
     *
     * @param ticketId Ticket ID
     * @param exitTime Exit timestamp
     * @return Completed Ticket
     */
    public Ticket unparkVehicle(String ticketId, LocalDateTime exitTime) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket ID cannot be null or empty.");
        }

        Optional<Ticket> ticketOpt = ticketDAO.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            throw new IllegalArgumentException("Ticket not found with ID: " + ticketId);
        }

        Ticket ticket = ticketOpt.get();
        if (ticket.isPaid()) {
            throw new IllegalStateException("Ticket " + ticketId + " has already been checked out and paid.");
        }

        // Reconstruct vehicle instance for pricing strategy multiplier calculation
        Vehicle vehicle = reconstructVehicle(ticket.getLicensePlate(), ticket.getVehicleType());

        double fee = pricingStrategy.calculateFee(ticket, vehicle, exitTime);
        ticket.setExitTime(exitTime);
        ticket.setFee(fee);
        ticket.setPaid(true);
        ticketDAO.save(ticket);

        // Vacate slot
        Optional<ParkingSlot> slotOpt = slotDAO.findById(ticket.getSlotId());
        if (slotOpt.isPresent()) {
            ParkingSlot slot = slotOpt.get();
            if (slot.isOccupied()) {
                slot.vacate();
                slotDAO.save(slot);
            }
        }

        return ticket;
    }

    public List<ParkingSlot> getAllSlots() {
        return slotDAO.findAll();
    }

    public List<Ticket> getAllTickets() {
        return ticketDAO.findAll();
    }

    public List<Ticket> getActiveTickets() {
        return ticketDAO.findAll().stream()
                .filter(t -> !t.isPaid())
                .toList();
    }

    private Vehicle reconstructVehicle(String licensePlate, VehicleType vehicleType) {
        return switch (vehicleType) {
            case BIKE -> new Bike(licensePlate);
            case CAR -> new Car(licensePlate);
            case TRUCK -> new Truck(licensePlate);
        };
    }
}
