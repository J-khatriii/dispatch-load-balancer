package com.jaswant.dispatch.repository;

import com.jaswant.dispatch.TestcontainersConfiguration;
import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Order;
import com.jaswant.dispatch.domain.Priority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    private static Order order(String id, double lat, double lon, double weight, Priority priority) {
        return new Order(id, new Location(lat, lon), "Address " + id, weight, priority);
    }

    @Test
    void findAllIsEmptyWhenNothingIsStored() {
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void upsertInsertsNewOrders() {
        var first = order("ORD001", 12.9716, 77.5946, 10, Priority.HIGH);
        var second = order("ORD002", 13.0827, 80.2707, 20, Priority.LOW);

        repository.upsertAll(List.of(first, second));

        assertThat(repository.findAll()).containsExactly(first, second);
    }

    @Test
    void upsertUpdatesAnExistingOrderInPlace() {
        repository.upsertAll(List.of(order("ORD001", 12.9716, 77.5946, 10, Priority.HIGH)));
        var changed = order("ORD001", 13.0827, 80.2707, 25, Priority.LOW);

        repository.upsertAll(List.of(changed));

        assertThat(repository.findAll()).containsExactly(changed);
    }

    @Test
    void upsertLeavesOtherOrdersUntouched() {
        var untouched = order("ORD001", 12.9716, 77.5946, 10, Priority.HIGH);
        repository.upsertAll(List.of(untouched, order("ORD002", 13.0827, 80.2707, 20, Priority.LOW)));
        var changed = order("ORD002", 13.0827, 80.2707, 99, Priority.MEDIUM);

        repository.upsertAll(List.of(changed));

        assertThat(repository.findAll()).containsExactly(untouched, changed);
    }

    @Test
    void upsertWithEmptyListIsANoOp() {
        repository.upsertAll(List.of());

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void upsertHandlesMoreOrdersThanOneBatch() {
        List<Order> orders = IntStream.range(0, 2_500)
                .mapToObj(i -> order("ORD%05d".formatted(i), 12.0, 77.0, 1, Priority.MEDIUM))
                .toList();

        repository.upsertAll(orders);

        assertThat(repository.findAll()).hasSize(2_500);
    }

    @Test
    void findAllIsOrderedByOrderId() {
        var later = order("ORD002", 12.0, 77.0, 5, Priority.LOW);
        var earlier = order("ORD001", 12.0, 77.0, 5, Priority.LOW);

        repository.upsertAll(List.of(later, earlier));

        assertThat(repository.findAll()).containsExactly(earlier, later);
    }
}