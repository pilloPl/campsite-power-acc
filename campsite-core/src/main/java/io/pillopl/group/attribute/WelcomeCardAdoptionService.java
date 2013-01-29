package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.persistence.dao.WelcomeCardAdoptionDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Whether a welcome card may be adopted at pedestals of a SITE group.
 */
@Service
public class WelcomeCardAdoptionService {

    private final WelcomeCardAdoptionDao welcomeCardAdoptionDao;

    public WelcomeCardAdoptionService(WelcomeCardAdoptionDao welcomeCardAdoptionDao) {
        this.welcomeCardAdoptionDao = welcomeCardAdoptionDao;
    }

    /**
     * Bubbles up the hierarchy: the nearest group with an attribute decides, no attribute at all means no.
     */
    @Transactional(readOnly = true)
    public boolean adoptionIsAllowedFor(Group group) {
        if (group != null) {
            WelcomeCardAdoption adoption = welcomeCardAdoptionDao.findForGroup(group.getId());
            if (adoption != null) {
                return adoption.isAllowed();
            } else {
                return adoptionIsAllowedFor(group.getParent());
            }
        } else {
            return false;
        }
    }
}
