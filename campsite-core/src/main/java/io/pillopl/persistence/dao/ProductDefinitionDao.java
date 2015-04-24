package io.pillopl.persistence.dao;

import io.pillopl.group.Market;
import io.pillopl.order.ProductDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDefinitionDao extends JpaRepository<ProductDefinition, Long> {

    ProductDefinition findByCodeAndDeletedFalse(String code);

    List<ProductDefinition> findByMarketAndDeletedFalseOrderByCode(Market market);
}
