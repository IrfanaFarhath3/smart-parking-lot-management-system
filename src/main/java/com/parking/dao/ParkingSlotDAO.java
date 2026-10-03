package com.parking.dao;

import com.parking.db.DatabaseManager;
import com.parking.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for ParkingSlot entities using SQLite JDBC.
 */
public class ParkingSlotDAO {

    public void save(ParkingSlot slot) {
        String sql = "INSERT OR REPLACE INTO slots (slot_id, slot_type, occupied, vehicle_license, vehicle_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slot.getSlotId());
            stmt.setString(2, slot.getSlotType().name());
            stmt.setInt(3, slot.isOccupied() ? 1 : 0);
            if (slot.getParkedVehicle() != null) {
                stmt.setString(4, slot.getParkedVehicle().getLicensePlate());
                stmt.setString(5, slot.getParkedVehicle().getType().name());
            } else {
                stmt.setNull(4, Types.VARCHAR);
                stmt.setNull(5, Types.VARCHAR);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving parking slot " + slot.getSlotId() + ": " + e.getMessage(), e);
        }
    }

    public Optional<ParkingSlot> findById(String slotId) {
        String sql = "SELECT slot_id, slot_type, occupied, vehicle_license, vehicle_type FROM slots WHERE slot_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSlot(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching parking slot " + slotId + ": " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<ParkingSlot> findAll() {
        List<ParkingSlot> slots = new ArrayList<>();
        String sql = "SELECT slot_id, slot_type, occupied, vehicle_license, vehicle_type FROM slots ORDER BY slot_id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                slots.add(mapResultSetToSlot(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all parking slots: " + e.getMessage(), e);
        }
        return slots;
    }

    public void saveAll(List<ParkingSlot> slots) {
        for (ParkingSlot slot : slots) {
            save(slot);
        }
    }

    public void deleteAll() {
        String sql = "DELETE FROM slots";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting all slots: " + e.getMessage(), e);
        }
    }

    private ParkingSlot mapResultSetToSlot(ResultSet rs) throws SQLException {
        String slotId = rs.getString("slot_id");
        SlotType slotType = SlotType.valueOf(rs.getString("slot_type"));
        boolean occupied = rs.getInt("occupied") == 1;
        String vehicleLicense = rs.getString("vehicle_license");
        String vehicleTypeStr = rs.getString("vehicle_type");

        Vehicle parkedVehicle = null;
        if (vehicleLicense != null && vehicleTypeStr != null) {
            VehicleType vType = VehicleType.valueOf(vehicleTypeStr);
            parkedVehicle = switch (vType) {
                case BIKE -> new Bike(vehicleLicense);
                case CAR -> new Car(vehicleLicense);
                case TRUCK -> new Truck(vehicleLicense);
            };
        }

        return new ParkingSlot(slotId, slotType, occupied, parkedVehicle);
    }
}
