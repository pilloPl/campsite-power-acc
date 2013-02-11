package io.pillopl.infrastructure;

import io.pillopl.persistence.dao.LocationDao;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class LocationIntegrationTest extends IntegrationTest {

    @Autowired
    LocationDao locationDao;

    @Autowired
    EntityManager entityManager;

    @Test
    void unknownLocationIsSeededForPedestalsInStock() {
        Location unknown = locationDao.findFirstByNameAndDeletedFalseOrderById(Location.UNKNOWN);

        assertThat(unknown.getId()).isEqualTo(1L);
        assertThat(unknown.isUnknown()).isTrue();
    }

    @Test
    void campsiteAddressIsStoredWithItsReceptionHours() {
        Location bay = new Location("Rovinj", "52210", "Obala", "12");
        bay.setName("Bay campsite");
        bay.setCountryIso("HR");
        bay.addReceptionHours(new ReceptionHours(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        bay.addReceptionHours(new ReceptionHours(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(20, 0)));
        Long id = locationDao.saveAndFlush(bay).getId();
        entityManager.clear();

        Location reloaded = locationDao.findById(id).orElseThrow();

        assertThat(id).isGreaterThanOrEqualTo(1000L);
        assertThat(reloaded.getReceptionHours()).hasSize(2);
        assertThat(reloaded.toAddressLine()).isEqualTo("Obala 12, 52210 Rovinj, HR");
    }
}
