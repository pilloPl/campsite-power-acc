package io.pillopl.persistence.dao;

import io.pillopl.card.LocalListVersion;
import io.pillopl.infrastructure.Pedestal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalListVersionDao extends JpaRepository<LocalListVersion, Long> {

    LocalListVersion findByPedestal(Pedestal pedestal);
}
