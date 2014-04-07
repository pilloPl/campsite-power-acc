package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.DefaultTariffDao;
import io.pillopl.pricing.Tariff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default tariff of a SITE subtree: the nearest {@link DefaultTariff} on the parent line.
 */
@Service
public class DefaultTariffService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTariffService.class);

    private final GroupService groupService;

    private final DefaultTariffDao defaultTariffDao;

    public DefaultTariffService(GroupService groupService, DefaultTariffDao defaultTariffDao) {
        this.groupService = groupService;
        this.defaultTariffDao = defaultTariffDao;
    }

    @Transactional(readOnly = true)
    public DefaultTariff getDefaultTariff(Long groupId) {
        List<Long> parentGroupLineIds = groupService.fetchParentLineIds(groupId, true);
        if (parentGroupLineIds.isEmpty()) {
            return null;
        }
        List<DefaultTariff> list = defaultTariffDao.findByGroupIds(parentGroupLineIds);
        return GroupAttributeHelper.findNearest(parentGroupLineIds, list);
    }

    /**
     * @return the tariff of the nearest attribute, or null
     */
    @Transactional(readOnly = true)
    public Tariff findDefaultTariff(Long groupId) {
        DefaultTariff defaultTariff = getDefaultTariff(groupId);
        return defaultTariff != null ? defaultTariff.getTariff() : null;
    }

    /**
     * Sets (or replaces) the default tariff of a SITE group. The tariff must belong to the market of the group.
     *
     * @param tariff null removes the attribute of the group (the parent's one applies again)
     */
    @Transactional
    public DefaultTariff setDefaultTariff(Group group, Tariff tariff) {
        if (!GroupType.SITE.isGroupType(group)) {
            throw new IllegalArgumentException(String.format("Group %s is no SITE group", group.getId()));
        }
        DefaultTariff existing = defaultTariffDao.findByGroupId(group.getId());
        if (tariff == null) {
            if (existing != null) {
                defaultTariffDao.delete(existing);
                LOG.info(String.format("Default tariff removed from group %s", group.getId()));
            }
            return null;
        }
        Market market = groupService.determineMarketByGroupId(group.getId());
        if (market != tariff.getMarket()) {
            throw new IllegalArgumentException(String.format("Tariff '%s' of market %s cannot be used in group %s "
                    + "of market %s", tariff.getName(), tariff.getMarket(), group.getId(), market));
        }
        DefaultTariff attribute = existing != null ? existing : new DefaultTariff();
        attribute.setGroup(group);
        attribute.setTariff(tariff);
        LOG.info(String.format("Default tariff of group %s set to '%s'", group.getId(), tariff.getName()));
        return defaultTariffDao.save(attribute);
    }
}
