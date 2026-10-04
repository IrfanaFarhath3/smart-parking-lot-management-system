package com.parking.service;

import com.parking.db.DatabaseManager;
import com.parking.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ParkingLotServiceTest {

    private ParkingLotService service;

    @BeforeEach
    void setUp() {
        // Use test DB for testing
        DatabaseManager.setDbUrl("jdbc:sqlite:target/test_parking_lot.db");
        DatabaseManager.initializeDatabase();
        service = new ParkingLotService();
        service.setupParkingLot(2, 2, 1); // 2 Bike, 2 Compact, 1 Large
    }

    @Test
    @DisplayName("Test parking a vehicle successfully")
    void testParkVehicleSuccess() {
        Vehicle car = new Car("MH-12-1234");
        Ticket ticket = service.parkVehicle(car);

        assertNotNull(ticket);
        assertEquals("MH-12-1234", ticket.getLicensePlate());
        assertEquals("C-01", ticket.getSlotId());
        assertFalse(ticket.isPaid());

        assertEquals(1, service.getActiveTickets().size());
    }

    @Test
    @DisplayName("Test unparking a vehicle and fee calculation")
    void testUnparkVehicleSuccess() {
        Vehicle bike = new Bike("MH-14-9999");
        Ticket ticket = service.parkVehicle(bike);

        LocalDateTime exitTime = ticket.getEntryTime().plusHours(2);
        Ticket completedTicket = service.unparkVehicle(ticket.getTicketId(), exitTime);

        assertTrue(completedTicket.isPaid());
        assertNotNull(completedTicket.getExitTime());
        // 2 hours * $10 * 0.5 = $10.0
        assertEquals(10.0, completedTicket.getFee(), 0.001);
        assertEquals(0, service.getActiveTickets().size());
    }

    @Test
    @DisplayName("Test preventing duplicate parking for same license plate")
    void testPreventDuplicateParking() {
        Vehicle car = new Car("KA-01-7777");
        service.parkVehicle(car);

        assertThrows(IllegalStateException.class, () -> service.parkVehicle(car));
    }

    @Test
    @DisplayName("Test parking lot full scenario")
    void testParkingLotFull() {
        Vehicle car1 = new Car("CAR-1");
        Vehicle car2 = new Car("CAR-2");
        Vehicle car3 = new Car("CAR-3"); // Fits in Large slot
        Vehicle car4 = new Car("CAR-4"); // Should fail

        service.parkVehicle(car1);
        service.parkVehicle(car2);
        service.parkVehicle(car3);

        assertThrows(IllegalStateException.class, () -> service.parkVehicle(car4));
    }

    @Test
    @DisplayName("Test parking null vehicle throws IllegalArgumentException")
    void testParkNullVehicle() {
        assertThrows(IllegalArgumentException.class, () -> service.parkVehicle(null));
    }

    @Test
    @DisplayName("Test unparking non-existent ticket throws IllegalArgumentException")
    void testUnparkNonExistentTicket() {
        assertThrows(IllegalArgumentException.class, () -> service.unparkVehicle("INVALID-TKT"));
        assertThrows(IllegalArgumentException.class, () -> service.unparkVehicle(null));
        assertThrows(IllegalArgumentException.class, () -> service.unparkVehicle("   "));
    }

    @Test
    @DisplayName("Test unparking an already paid ticket throws IllegalStateException")
    void testUnparkAlreadyPaidTicket() {
        Vehicle car = new Car("KA-02-1111");
        Ticket ticket = service.parkVehicle(car);

        service.unparkVehicle(ticket.getTicketId());

        assertThrows(IllegalStateException.class, () -> service.unparkVehicle(ticket.getTicketId()));
    }
}
