package io.pillopl.persistence.dao;

import io.pillopl.power.PowerSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PowerSectionDao extends JpaRepository<PowerSection, Long>, PowerSectionDaoCustom {

    PowerSection findBySectionName(String sectionName);

    List<PowerSection> findAllByOrderBySectionName();
}
