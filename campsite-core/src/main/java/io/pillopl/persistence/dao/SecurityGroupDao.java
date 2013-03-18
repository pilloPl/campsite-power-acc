package io.pillopl.persistence.dao;

import io.pillopl.user.SecurityGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityGroupDao extends JpaRepository<SecurityGroup, Long> {

    SecurityGroup findByName(String name);
}
