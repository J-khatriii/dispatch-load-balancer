package com.jaswant.dispatch.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LocationTest {

    private static final Location DELHI = new Location(28.6139, 77.2090);
    private static final Location MUMBAI = new Location(19.0760, 72.8777);

    @Test
    void distanceToSelfIsZero() {
        assertThat(DELHI.distanceKmTo(DELHI)).isZero();
    }

    @Test
    void distanceBetweenCitiesIsRealistic() {
        assertThat(DELHI.distanceKmTo(MUMBAI)).isCloseTo(1150, within(10.0));
    }
}