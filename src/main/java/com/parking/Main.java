package com.parking;

import com.parking.db.DatabaseManager;
import com.parking.model.*;
import com.parking.service.ParkingLotService;
import com.parking.web.WebServer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Main application class launching both the Embedded Web REST Server and the interactive CLI Console.
 */
public class Main {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // Step 1: Initialize database
        DatabaseManager.initializeDatabase();
        ParkingLotService parkingService = new ParkingLotService();

        // If parking lot is empty, initialize default layout
        if (parkingService.getAllSlots().isEmpty()) {
            parkingService.setupParkingLot(5, 10, 5); // 5 Bike, 10 Compact, 5 Large
        }

        // Step 2: Start Embedded Web REST API Server on PORT env var (default 8080)
        WebServer webServer = null;
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        try {
            webServer = new WebServer(port, parkingService);
            webServer.start();
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not start Embedded Web Server on port " + port + ": " + e.getMessage());
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=================================================");
        System.out.println("   WELCOME TO SMART PARKING LOT MANAGEMENT SYSTEM ");
        System.out.println("=================================================");
        System.out.println("🌐 Visual Web UI live at: http://localhost:" + port + " (0.0.0.0:" + port + ")");

        while (running) {
            printMenu();
            System.out.print("Select an option (1-7): ");
            if (!scanner.hasNextLine()) {
                System.out.println("No interactive input available. Continuing in web-only mode.");
                break;
            }
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> handleParkVehicle(scanner, parkingService);
                case "2" -> handleUnparkVehicle(scanner, parkingService);
                case "3" -> handleViewStatus(parkingService);
                case "4" -> handleViewActiveTickets(parkingService);
                case "5" -> handleViewAllTickets(parkingService);
                case "6" -> handleReconfigureLot(scanner, parkingService);
                case "7" -> {
                    running = false;
                    System.out.println("\nStopping Web Server and exiting system...");
                    if (webServer != null) {
                        webServer.stop();
                    }
                    System.out.println("Goodbye!");
                }
                default -> System.out.println("\n❌ Invalid choice! Please select a number between 1 and 7.");
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n-------------------------------------------------");
        System.out.println(" 1. Park Vehicle");
        System.out.println(" 2. Unpark Vehicle / Checkout");
        System.out.println(" 3. View Parking Lot Layout & Status");
        System.out.println(" 4. View Active Tickets");
        System.out.println(" 5. View Full Ticket History");
        System.out.println(" 6. Setup / Reset Parking Lot Layout");
        System.out.println(" 7. Exit");
        System.out.println("-------------------------------------------------");
    }

    private static void handleParkVehicle(Scanner scanner, ParkingLotService service) {
        System.out.println("\n--- 🚗 PARK VEHICLE ---");
        System.out.print("Enter License Plate Number (e.g. MH-12-AB-1234): ");
        String licensePlate = scanner.nextLine().trim();

        if (licensePlate.isEmpty()) {
            System.out.println("❌ License plate cannot be empty.");
            return;
        }

        System.out.println("Select Vehicle Type:");
        System.out.println("  1. Bike");
        System.out.println("  2. Car");
        System.out.println("  3. Truck");
        System.out.print("Choice (1-3): ");
        String typeChoice = scanner.nextLine().trim();

        Vehicle vehicle = switch (typeChoice) {
            case "1" -> new Bike(licensePlate);
            case "2" -> new Car(licensePlate);
            case "3" -> new Truck(licensePlate);
            default -> null;
        };

        if (vehicle == null) {
            System.out.println("❌ Invalid vehicle type selected.");
            return;
        }

        try {
            Ticket ticket = service.parkVehicle(vehicle);
            System.out.println("\n✅ Vehicle Parked Successfully!");
            System.out.println("   Ticket ID     : " + ticket.getTicketId());
            System.out.println("   Allocated Slot: " + ticket.getSlotId());
            System.out.println("   License Plate : " + ticket.getLicensePlate());
            System.out.println("   Entry Time    : " + ticket.getEntryTime().format(FORMATTER));
        } catch (Exception e) {
            System.out.println("\n❌ Parking Failed: " + e.getMessage());
        }
    }

    private static void handleUnparkVehicle(Scanner scanner, ParkingLotService service) {
        System.out.println("\n--- 💳 UNPARK VEHICLE / CHECKOUT ---");
        System.out.print("Enter Ticket ID or License Plate: ");
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println("❌ Input cannot be empty.");
            return;
        }

        String ticketId = query;
        if (!query.toUpperCase().startsWith("TKT-")) {
            var activeTicketOpt = service.getActiveTickets().stream()
                    .filter(t -> t.getLicensePlate().equalsIgnoreCase(query))
                    .findFirst();

            if (activeTicketOpt.isPresent()) {
                ticketId = activeTicketOpt.get().getTicketId();
            } else {
                System.out.println("❌ No active parking record found for License Plate / Ticket ID: " + query);
                return;
            }
        }

        System.out.print("Simulate parking duration in hours? (Press Enter for current real-time exit, or enter hours e.g. 3): ");
        String hoursInput = scanner.nextLine().trim();

        LocalDateTime exitTime = LocalDateTime.now();
        if (!hoursInput.isEmpty()) {
            try {
                long hours = Long.parseLong(hoursInput);
                var activeTicket = service.getActiveTickets().stream()
                        .filter(t -> t.getTicketId().equalsIgnoreCase(query) || t.getLicensePlate().equalsIgnoreCase(query))
                        .findFirst();
                if (activeTicket.isPresent()) {
                    exitTime = activeTicket.get().getEntryTime().plusHours(hours);
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid hours value. Using current system timestamp.");
            }
        }

        try {
            Ticket ticket = service.unparkVehicle(ticketId, exitTime);
            System.out.println("\n✅ Vehicle Unparked & Receipt Generated!");
            System.out.println("   Ticket ID    : " + ticket.getTicketId());
            System.out.println("   License Plate: " + ticket.getLicensePlate());
            System.out.println("   Slot Vacated : " + ticket.getSlotId());
            System.out.println("   Entry Time   : " + ticket.getEntryTime().format(FORMATTER));
            System.out.println("   Exit Time    : " + ticket.getExitTime().format(FORMATTER));
            System.out.println("   -------------------------------------");
            System.out.println("   TOTAL FEE    : $" + String.format("%.2f", ticket.getFee()));
            System.out.println("   STATUS       : PAID");
        } catch (Exception e) {
            System.out.println("\n❌ Unparking Failed: " + e.getMessage());
        }
    }

    private static void handleViewStatus(ParkingLotService service) {
        System.out.println("\n--- 🅿️ PARKING LOT LAYOUT & STATUS ---");
        List<ParkingSlot> slots = service.getAllSlots();

        if (slots.isEmpty()) {
            System.out.println("Parking lot is empty / unconfigured.");
            return;
        }

        long total = slots.size();
        long occupied = slots.stream().filter(ParkingSlot::isOccupied).count();
        long available = total - occupied;

        System.out.printf("Total Capacity: %d | Occupied: %d | Available: %d%n", total, occupied, available);
        System.out.println("-------------------------------------------------");
        System.out.printf("%-10s | %-10s | %-10s | %-15s%n", "SLOT ID", "SLOT TYPE", "STATUS", "PARKED VEHICLE");
        System.out.println("-------------------------------------------------");

        for (ParkingSlot slot : slots) {
            String status = slot.isOccupied() ? "🔴 OCCUPIED" : "🟢 VACANT";
            String vehicleInfo = slot.isOccupied() && slot.getParkedVehicle() != null ? 
                    slot.getParkedVehicle().getLicensePlate() + " (" + slot.getParkedVehicle().getType() + ")" : "-";
            System.out.printf("%-10s | %-10s | %-10s | %-15s%n", 
                    slot.getSlotId(), slot.getSlotType(), status, vehicleInfo);
        }
    }

    private static void handleViewActiveTickets(ParkingLotService service) {
        System.out.println("\n--- 🎫 ACTIVE TICKETS ---");
        List<Ticket> activeTickets = service.getActiveTickets();

        if (activeTickets.isEmpty()) {
            System.out.println("No active parking sessions at the moment.");
            return;
        }

        System.out.printf("%-15s | %-12s | %-10s | %-8s | %-19s%n", 
                "TICKET ID", "LICENSE", "VEHICLE", "SLOT", "ENTRY TIME");
        System.out.println("-----------------------------------------------------------------");
        for (Ticket t : activeTickets) {
            System.out.printf("%-15s | %-12s | %-10s | %-8s | %-19s%n", 
                    t.getTicketId(), t.getLicensePlate(), t.getVehicleType(), t.getSlotId(), t.getEntryTime().format(FORMATTER));
        }
    }

    private static void handleViewAllTickets(ParkingLotService service) {
        System.out.println("\n--- 📜 FULL TICKET HISTORY ---");
        List<Ticket> tickets = service.getAllTickets();

        if (tickets.isEmpty()) {
            System.out.println("No ticket history recorded yet.");
            return;
        }

        System.out.printf("%-13s | %-10s | %-8s | %-6s | %-19s | %-19s | %-8s | %-6s%n", 
                "TICKET ID", "LICENSE", "TYPE", "SLOT", "ENTRY TIME", "EXIT TIME", "FEE", "PAID");
        System.out.println("---------------------------------------------------------------------------------------------------");
        for (Ticket t : tickets) {
            String exitStr = t.getExitTime() != null ? t.getExitTime().format(FORMATTER) : "-";
            System.out.printf("%-13s | %-10s | %-8s | %-6s | %-19s | %-19s | $%-7.2f | %-6s%n", 
                    t.getTicketId(), t.getLicensePlate(), t.getVehicleType(), t.getSlotId(), 
                    t.getEntryTime().format(FORMATTER), exitStr, t.getFee(), t.isPaid() ? "YES" : "NO");
        }
    }

    private static void handleReconfigureLot(Scanner scanner, ParkingLotService service) {
        System.out.println("\n--- ⚙️ RECONFIGURE PARKING LOT ---");
        System.out.println("⚠️ Warning: This will clear existing parking lot configuration!");
        System.out.print("Enter number of Bike slots: ");
        int bike = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter number of Compact slots: ");
        int compact = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter number of Large slots: ");
        int large = Integer.parseInt(scanner.nextLine().trim());

        service.setupParkingLot(bike, compact, large);
        System.out.println("✅ Parking lot reconfigured with " + bike + " Bike, " + compact + " Compact, and " + large + " Large slots.");
    }
}
