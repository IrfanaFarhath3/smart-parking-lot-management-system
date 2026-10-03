package com.parking.dao;

import com.parking.db.DatabaseManager;
import com.parking.model.Ticket;
import com.parking.model.VehicleType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Ticket entities using SQLite JDBC.
 */
public class TicketDAO {

    public void save(Ticket ticket) {
        String sql = "INSERT OR REPLACE INTO tickets (ticket_id, license_plate, vehicle_type, slot_id, entry_time, exit_time, fee, paid) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticket.getTicketId());
            stmt.setString(2, ticket.getLicensePlate());
            stmt.setString(3, ticket.getVehicleType().name());
            stmt.setString(4, ticket.getSlotId());
            stmt.setString(5, ticket.getEntryTime().toString());
            if (ticket.getExitTime() != null) {
                stmt.setString(6, ticket.getExitTime().toString());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            stmt.setDouble(7, ticket.getFee());
            stmt.setInt(8, ticket.isPaid() ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving ticket " + ticket.getTicketId() + ": " + e.getMessage(), e);
        }
    }

    public Optional<Ticket> findById(String ticketId) {
        String sql = "SELECT ticket_id, license_plate, vehicle_type, slot_id, entry_time, exit_time, fee, paid " +
                     "FROM tickets WHERE ticket_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticketId.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ticket " + ticketId + ": " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Ticket> findActiveByLicensePlate(String licensePlate) {
        String sql = "SELECT ticket_id, license_plate, vehicle_type, slot_id, entry_time, exit_time, fee, paid " +
                     "FROM tickets WHERE license_plate = ? AND paid = 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, licensePlate.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding active ticket for license plate " + licensePlate + ": " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Ticket> findAll() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT ticket_id, license_plate, vehicle_type, slot_id, entry_time, exit_time, fee, paid " +
                     "FROM tickets ORDER BY entry_time DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all tickets: " + e.getMessage(), e);
        }
        return tickets;
    }

    public void deleteAll() {
        String sql = "DELETE FROM tickets";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting all tickets: " + e.getMessage(), e);
        }
    }

    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        String ticketId = rs.getString("ticket_id");
        String licensePlate = rs.getString("license_plate");
        VehicleType vehicleType = VehicleType.valueOf(rs.getString("vehicle_type"));
        String slotId = rs.getString("slot_id");
        LocalDateTime entryTime = LocalDateTime.parse(rs.getString("entry_time"));
        
        String exitTimeStr = rs.getString("exit_time");
        LocalDateTime exitTime = (exitTimeStr != null) ? LocalDateTime.parse(exitTimeStr) : null;
        
        double fee = rs.getDouble("fee");
        boolean paid = rs.getInt("paid") == 1;

        return new Ticket(ticketId, licensePlate, vehicleType, slotId, entryTime, exitTime, fee, paid);
    }
}
