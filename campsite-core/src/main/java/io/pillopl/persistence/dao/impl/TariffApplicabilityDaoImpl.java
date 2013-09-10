package io.pillopl.persistence.dao.impl;

import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.TariffApplicabilityDaoCustom;
import io.pillopl.pricing.TariffApplicability;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Active tariff assignment by Criteria API: the newest start date not after the time asked for.
 */
public class TariffApplicabilityDaoImpl implements TariffApplicabilityDaoCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public TariffApplicability findActiveTariffForSocket(Long socketId, LocalDateTime at) {
        return findActive("socket", socketId, at);
    }

    @Override
    public TariffApplicability findActiveTariffForPedestal(Long pedestalId, LocalDateTime at) {
        return findActive("pedestal", pedestalId, at);
    }

    private TariffApplicability findActive(String target, Long targetId, LocalDateTime at) {
        if (targetId == null || at == null) {
            return null;
        }
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<TariffApplicability> query = builder.createQuery(TariffApplicability.class);
        Root<TariffApplicability> tariffApplicability = query.from(TariffApplicability.class);
        Join<TariffApplicability, ?> joined = "socket".equals(target)
                ? tariffApplicability.<TariffApplicability, Socket>join("socket")
                : tariffApplicability.<TariffApplicability, Pedestal>join("pedestal");
        // return only the most recent assignment, if there is any
        CriteriaQuery<TariffApplicability> q = query.where(builder.and(
                        builder.equal(joined.get("id"), targetId),
                        builder.lessThanOrEqualTo(tariffApplicability.<LocalDateTime>get("startDate"), at)))
                .orderBy(builder.desc(tariffApplicability.get("startDate")), builder.desc(tariffApplicability.get("id")));
        List<TariffApplicability> result = em.createQuery(q).setMaxResults(1).getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }
}
