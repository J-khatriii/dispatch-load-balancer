package com.jaswant.dispatch.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineTest {

    @Test
    void samePointHasZeroDistance() {
        assertThat(Haversine.distanceKm(28.6139, 77.2090, 28.6139, 77.2090)).isZero();
    }

    @Test
    void delhiToMumbaiIsAboutOneThousandOneHundredFortyEightKm() {
        double km = Haversine.distanceKm(28.6139, 77.2090, 19.0760, 72.8777);
        assertThat(km).isCloseTo(1148, within(5.0));
    }

    @Test
    void distanceIsSymmetric() {
        double ab = Haversine.distanceKm(28.6139, 77.2090, 28.5355, 77.3910);
        double ba = Haversine.distanceKm(28.5355, 77.3910, 28.6139, 77.2090);
        assertThat(ab).isEqualTo(ba);
    }
}
