package com.jaswant.dispatch.repository;

import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Vehicle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class VehicleRepository {

    private static final int BATCH_SIZE = 1_000;

    private static final String UPSERT = """
            INSERT INTO vehicles (vehicle_id, capacity, current_latitude, current_longitude, current_address)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (vehicle_id) DO UPDATE SET
                capacity = EXCLUDED.capacity,
                current_latitude = EXCLUDED.current_latitude,
                current_longitude = EXCLUDED.current_longitude,
                current_address = EXCLUDED.current_address
            """;

    // Stable ordering keeps dispatch plans deterministic between calls.
    private static final String SELECT_ALL = """
            SELECT vehicle_id, capacity, current_latitude, current_longitude, current_address
            FROM vehicles
            ORDER BY vehicle_id
            """;

    private final JdbcTemplate jdbc;

    public VehicleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertAll(List<Vehicle> vehicles) {
        jdbc.batchUpdate(UPSERT, vehicles, BATCH_SIZE, (statement, vehicle) -> {
            statement.setString(1, vehicle.vehicleId());
            statement.setDouble(2, vehicle.capacity());
            statement.setDouble(3, vehicle.location().latitude());
            statement.setDouble(4, vehicle.location().longitude());
            statement.setString(5, vehicle.address());
        });
    }

    public List<Vehicle> findAll() {
        return jdbc.query(SELECT_ALL, VehicleRepository::mapRow);
    }

    private static Vehicle mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Vehicle(
                rs.getString("vehicle_id"),
                rs.getDouble("capacity"),
                new Location(rs.getDouble("current_latitude"), rs.getDouble("current_longitude")),
                rs.getString("current_address"));
    }
}