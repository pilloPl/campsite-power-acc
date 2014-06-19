package io.pillopl.persistence.dao;

import io.pillopl.power.PowerSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PowerSectionDao extends JpaRepository<PowerSection, Long> {

    PowerSection findBySectionName(String sectionName);
}
