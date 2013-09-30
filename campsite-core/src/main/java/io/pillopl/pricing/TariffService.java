package io.pillopl.pricing;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.DefaultTariffDao;
import io.pillopl.persistence.dao.TariffApplicabilityDao;
import io.pillopl.persistence.dao.TariffDao;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Tariffs of a market: list, lookup, create, change while unused, market default. Users see and change the tariffs
 * of their own market.
 */
@Service
@Transactional(readOnly = true)
public class TariffService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TariffService.class);

    private final UserinfoService userinfoService;

    private final TariffDao tariffDao;

    private final TariffApplicabilityDao tariffApplicabilityDao;

    private final DefaultTariffDao defaultTariffDao;

    public TariffService(UserinfoService userinfoService, TariffDao tariffDao,
                         TariffApplicabilityDao tariffApplicabilityDao, DefaultTariffDao defaultTariffDao) {
        this.userinfoService = userinfoService;
        this.tariffDao = tariffDao;
        this.tariffApplicabilityDao = tariffApplicabilityDao;
        this.defaultTariffDao = defaultTariffDao;
    }

    public List<Tariff> getAllTariffs(Userinfo principal) {
        LOGGER.debug("Getting all tariffs for: " + principal.getUsername());
        return tariffDao.findAllForMarket(evaluateMarket(principal));
    }

    public Tariff findTariff(Long tariffId) {
        return tariffId != null ? tariffDao.findById(tariffId).orElse(null) : null;
    }

    /**
     * @return the tariff or null when it does not exist
     * @throws AccessDeniedException when the tariff belongs to another market than the user
     */
    public Tariff findTariffForUser(Userinfo principal, Long tariffId) {
        Tariff tariff = findTariff(tariffId);
        if (tariff != null && tariff.getMarket() != evaluateMarket(principal)) {
            throw new AccessDeniedException(String.format("Tariff %s is not of the market of '%s'",
                    tariffId, principal.getUsername()));
        }
        return tariff;
    }

    public Tariff getDefaultTariff(Market market) {
        LOGGER.debug("Getting default tariff for: " + market);
        Tariff defaultTariffForMarket = tariffDao.findDefaultTariffForMarket(market);
        if (defaultTariffForMarket == null) {
            LOGGER.debug("Could not find default tariff for: " + market);
        }
        return defaultTariffForMarket;
    }

    public boolean existsTariff(Market market, String tariffName) {
        LOGGER.debug(String.format("Checking if tariff %s is unique for %s", tariffName, market));
        return tariffDao.findForNameAndMarket(market, tariffName) != null;
    }

    /**
     * A tariff is in use when it is assigned to a socket or pedestal, is the default of a campsite or of the market.
     * Used tariffs are not changed (records keep their meaning); a new tariff is assigned from a start date instead.
     */
    public boolean isInUse(Tariff tariff) {
        return tariff.isDefaultTariff() || tariffApplicabilityDao.countByTariffId(tariff.getId()) > 0
                || defaultTariffDao.countByTariffId(tariff.getId()) > 0;
    }

    /**
     * Creates a tariff in the market of the user (or the given market when the user may see it).
     */
    @Transactional
    public Tariff saveTariff(Userinfo principal, Tariff tariff) {
        LOGGER.debug(String.format("Saving tariff %s for %s", tariff.getName(), principal.getUsername()));
        if (tariff.getMarket() == null) {
            tariff.setMarket(evaluateMarket(principal));
        } else if (tariff.getMarket() != evaluateMarket(principal)) {
            throw new AccessDeniedException(String.format("User '%s' cannot create tariffs of "
                    + "market %s", principal.getUsername(), tariff.getMarket()));
        }
        validateTariff(tariff);
        if (existsTariff(tariff.getMarket(), tariff.getName())) {
            logAndThrowException(String.format("Tariff with name %s already exists for %s", tariff.getName(),
                    tariff.getMarket()));
        }
        if (tariff.isDefaultTariff()) {
            Tariff defaultTariffForMarket = tariffDao.findDefaultTariffForMarket(tariff.getMarket());
            if (defaultTariffForMarket != null) {
                logAndThrowException(String.format("Default tariff for %s already defined", tariff.getMarket()));
            }
        }
        tariff.fillTariffDetails();
        tariff.setLastUpdateTime(LocalDateTime.now());
        return tariffDao.save(tariff);
    }

    /**
     * Replaces description, currency and elements of an unused tariff.
     */
    @Transactional
    public Tariff updateTariff(Userinfo principal, Long tariffId, Tariff changes) {
        Tariff tariff = findTariffForUser(principal, tariffId);
        if (tariff == null) {
            throw new NoSuchElementException("Tariff not found: " + tariffId);
        }
        if (isInUse(tariff)) {
            logAndThrowException(String.format("Tariff %s is in use: create a new tariff and assign it from a start "
                    + "date", tariff.getName()));
        }
        changes.setName(tariff.getName());
        validateTariff(changes);
        tariff.setDescription(changes.getDescription());
        tariff.setCurrency(changes.getCurrency());
        tariff.getTariffElements().clear();
        tariffDao.flush();
        for (TariffElement element : changes.getTariffElements()) {
            tariff.getTariffElements().add(element);
        }
        tariff.fillTariffDetails();
        tariff.setLastUpdateTime(LocalDateTime.now());
        return tariffDao.save(tariff);
    }

    /**
     * Makes the tariff the default of its market (the last step of the tariff resolution), replacing the old one.
     */
    @Transactional
    public Tariff setMarketDefaultTariff(Userinfo principal, Long tariffId) {
        Tariff tariff = findTariffForUser(principal, tariffId);
        if (tariff == null) {
            throw new NoSuchElementException("Tariff not found: " + tariffId);
        }
        Tariff previous = tariffDao.findDefaultTariffForMarket(tariff.getMarket());
        if (previous != null && !previous.getId().equals(tariff.getId())) {
            previous.setDefaultTariff(false);
            previous.setLastUpdateTime(LocalDateTime.now());
            // the database allows one default per market: the old one goes first
            tariffDao.saveAndFlush(previous);
        }
        tariff.setDefaultTariff(true);
        tariff.setLastUpdateTime(LocalDateTime.now());
        LOGGER.info(String.format("Default tariff of %s is now '%s'", tariff.getMarket(), tariff.getName()));
        return tariffDao.save(tariff);
    }

    /**
     * Content rules: a name, at least one element, every element with at least one component and at most one
     * component per dimension, prices not negative, step sizes positive, FLAT with step size 1, ISO currency.
     *
     * @throws IllegalArgumentException with the first broken rule
     */
    public static void validateTariff(Tariff tariff) {
        if (tariff.getName() == null || tariff.getName().isBlank()) {
            throw new IllegalArgumentException("Tariff name is missing");
        }
        if (tariff.getDescription() == null || tariff.getDescription().isBlank()) {
            tariff.setDescription(tariff.getName());
        }
        if (tariff.getCurrency() == null || !tariff.getCurrency().matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be an ISO code of three letters: "
                    + tariff.getCurrency());
        }
        if (tariff.getTariffElements().isEmpty()) {
            throw new IllegalArgumentException(String.format("Tariff %s has no elements", tariff.getName()));
        }
        int position = 0;
        for (TariffElement element : tariff.getTariffElements()) {
            if (element.getPriceComponents().isEmpty()) {
                throw new IllegalArgumentException(String.format("Element %d of tariff %s has no price components",
                        position, tariff.getName()));
            }
            Set<TariffDimensionType> dimensions = EnumSet.noneOf(TariffDimensionType.class);
            for (PriceComponent component : element.getPriceComponents()) {
                validateComponent(tariff, position, component);
                if (!dimensions.add(component.getDimensionType())) {
                    throw new IllegalArgumentException(String.format("Element %d of tariff %s has two %s components",
                            position, tariff.getName(), component.getDimensionType()));
                }
            }
            position++;
        }
    }

    private static void validateComponent(Tariff tariff, int position, PriceComponent component) {
        if (component.getDimensionType() == null) {
            throw new IllegalArgumentException(String.format("Element %d of tariff %s has a component without "
                    + "dimension", position, tariff.getName()));
        }
        if (component.getPrice() == null || component.getPrice().signum() < 0) {
            throw new IllegalArgumentException(String.format("Price of %s in element %d of tariff %s must not be "
                    + "negative", component.getDimensionType(), position, tariff.getName()));
        }
        if (component.getStepSize() == null || component.getStepSize() < 1) {
            throw new IllegalArgumentException(String.format("Step size of %s in element %d of tariff %s must be "
                    + "positive", component.getDimensionType(), position, tariff.getName()));
        }
        if (component.getDimensionType() == TariffDimensionType.FLAT && component.getStepSize() != 1) {
            throw new IllegalArgumentException(String.format("Flat fee in element %d of tariff %s has step size 1",
                    position, tariff.getName()));
        }
    }

    private Market evaluateMarket(Userinfo principal) {
        Market market = userinfoService.determineMarket(principal.getUsername());
        if (market == null || !market.isPrefixMarket()) {
            logAndThrowException(String.format("Cannot determine market for user %s", principal.getUsername()));
        }
        return market;
    }

    private static void logAndThrowException(String message) {
        LOGGER.error(message);
        throw new TariffOperationException(message);
    }
}
