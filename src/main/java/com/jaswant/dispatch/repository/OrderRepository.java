package com.jaswant.dispatch.repository;

import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Priority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class OrderRepository {

    private static final int BATCH_SIZE = 1_000;

    private static final String UPSERT = """
            INSERT INTO orders (order_id, latitude, longitude, address, package_weight, priority)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (order_id) DO UPDATE SET
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                address = EXCLUDED.address,
                package_weight = EXCLUDED.package_weight,
                priority = EXCLUDED.priority
            """;

    // Stable ordering keeps dispatch plans deterministic between calls.
    private static final String SELECT_ALL = """
            SELECT order_id, latitude, longitude, address, package_weight, priority
            FROM orders
            ORDER BY order_id
            """;

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertAll(List<Order> orders) {
        jdbc.batchUpdate(UPSERT, orders, BATCH_SIZE, (statement, order) -> {
            statement.setString(1, order.orderId());
            statement.setDouble(2, order.location().latitude());
            statement.setDouble(3, order.location().longitude());
            statement.setString(4, order.address());
            statement.setDouble(5, order.weight());
            statement.setString(6, order.priority().name());
        });
    }

    public List<Order> findAll() {
        return jdbc.query(SELECT_ALL, OrderRepository::mapRow);
    }

    private static Order mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Order(
                rs.getString("order_id"),
                new Location(rs.getDouble("latitude"), rs.getDouble("longitude")),
                rs.getString("address"),
                rs.getDouble("package_weight"),
                Priority.valueOf(rs.getString("priority")));
    }
}