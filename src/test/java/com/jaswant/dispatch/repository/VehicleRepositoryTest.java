package com.jaswant.dispatch.repository;

import com.jaswant.dispatch.TestcontainersConfiguration;
import com.jaswant.dispatch.domain.Location;
import com.jaswant.dispatch.domain.Vehicle;
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
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository repository;

    private static Vehicle vehicle(String id, double capacity, double lat, double lon) {
        return new Vehicle(id, capacity, new Location(lat, lon), "Depot " + id);
    }

    @Test
    void findAllIsEmptyWhenNothingIsStored() {
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void upsertInsertsNewVehicles() {
        var first = vehicle("VEH001", 100, 12.9716, 77.6413);
        var second = vehicle("VEH002", 150, 13.0674, 80.2376);

        repository.upsertAll(List.of(first, second));

        assertThat(repository.findAll()).containsExactly(first, second);
    }

    @Test
    void upsertUpdatesAnExistingVehicleInPlace() {
        repository.upsertAll(List.of(vehicle("VEH001", 100, 12.9716, 77.6413)));
        var changed = vehicle("VEH001", 250, 13.0674, 80.2376);

        repository.upsertAll(List.of(changed));

        assertThat(repository.findAll()).containsExactly(changed);
    }

    @Test
    void upsertLeavesOtherVehiclesUntouched() {
        var untouched = vehicle("VEH001", 100, 12.9716, 77.6413);
        repository.upsertAll(List.of(untouched, vehicle("VEH002", 150, 13.0674, 80.2376)));
        var changed = vehicle("VEH002", 300, 13.0674, 80.2376);

        repository.upsertAll(List.of(changed));

        assertThat(repository.findAll()).containsExactly(untouched, changed);
    }

    @Test
    void upsertWithEmptyListIsANoOp() {
        repository.upsertAll(List.of());

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void upsertHandlesMoreVehiclesThanOneBatch() {
        List<Vehicle> vehicles = IntStream.range(0, 2_500)
                .mapToObj(i -> vehicle("VEH%05d".formatted(i), 100, 12.0, 77.0))
                .toList();

        repository.upsertAll(vehicles);

        assertThat(repository.findAll()).hasSize(2_500);
    }

    @Test
    void findAllIsOrderedByVehicleId() {
        var later = vehicle("VEH002", 100, 12.0, 77.0);
        var earlier = vehicle("VEH001", 100, 12.0, 77.0);

        repository.upsertAll(List.of(later, earlier));

        assertThat(repository.findAll()).containsExactly(earlier, later);
    }
}