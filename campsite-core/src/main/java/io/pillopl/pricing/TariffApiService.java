package io.pillopl.pricing;

import io.pillopl.api.TariffApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.dto.pricing.PriceSimulationDTO;
import io.pillopl.dto.pricing.SocketTariffDTO;
import io.pillopl.dto.pricing.TariffApplicabilityDTO;
import io.pillopl.dto.pricing.TariffDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.DefaultTariffService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.value.GroupId;
import io.pillopl.value.Kwh;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.SocketId;
import io.pillopl.value.TariffId;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tariff calls of the console on top of {@link TariffService}, {@link TariffApplicabilityService},
 * {@link TariffResolver} and {@link PricingService}. Viewing and simulating need PEDESTAL_VIEW, tariff changes and
 * assignments TARIFF_EDIT, the market default CONFIG_EDIT. Tariffs are those of the caller's market; pedestals,
 * sockets and campsites must lie in the caller's SITE scope.
 */
@Service
@Transactional
public class TariffApiService implements TariffApi {

    private final TariffService tariffService;

    private final TariffApplicabilityService tariffApplicabilityService;

    private final TariffResolver tariffResolver;

    private final PricingService pricingService;

    private final DefaultTariffService defaultTariffService;

    private final PedestalService pedestalService;

    private final GroupService groupService;

    private final UserinfoService userinfoService;

    private final PrincipalResolver principalResolver;

    public TariffApiService(TariffService tariffService, TariffApplicabilityService tariffApplicabilityService,
                            TariffResolver tariffResolver, PricingService pricingService,
                            DefaultTariffService defaultTariffService, PedestalService pedestalService,
                            GroupService groupService, UserinfoService userinfoService,
                            PrincipalResolver principalResolver) {
        this.tariffService = tariffService;
        this.tariffApplicabilityService = tariffApplicabilityService;
        this.tariffResolver = tariffResolver;
        this.pricingService = pricingService;
        this.defaultTariffService = defaultTariffService;
        this.pedestalService = pedestalService;
        this.groupService = groupService;
        this.userinfoService = userinfoService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TariffDTO> findTariffs(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            return TariffMapper.toDTOs(tariffService.getAllTariffs(principal));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TariffDTO getTariff(Username authUsername, TariffId tariffId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            return TariffMapper.toDTO(fetchTariff(principal, tariffId));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TariffDTO getDefaultTariff(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            return TariffMapper.toDTO(tariffService.getDefaultTariff(principal));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsTariff(Username authUsername, String tariffName) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            if (tariffName == null || tariffName.isBlank()) {
                throw new InvalidInputException("Tariff name is missing");
            }
            return tariffService.existsTariff(principal, tariffName.trim());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public TariffDTO saveTariff(Username authUsername, TariffDTO tariff) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.TARIFF_EDIT);
            if (tariff == null) {
                throw new InvalidInputException("Tariff is missing");
            }
            return TariffMapper.toDTO(tariffService.saveTariff(principal, TariffMapper.toEntity(tariff)));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public TariffDTO updateTariff(Username authUsername, TariffId tariffId, TariffDTO tariff) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.TARIFF_EDIT);
            if (tariff == null) {
                throw new InvalidInputException("Tariff is missing");
            }
            fetchTariff(principal, tariffId);
            return TariffMapper.toDTO(tariffService.updateTariff(principal, tariffId.value(),
                    TariffMapper.toEntity(tariff)));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public TariffDTO setMarketDefaultTariff(Username authUsername, TariffId tariffId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            fetchTariff(principal, tariffId);
            return TariffMapper.toDTO(tariffService.setMarketDefaultTariff(principal, tariffId.value()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public List<TariffApplicabilityDTO> applyTariffToPedestals(Username authUsername, List<PedestalCode> pedestalCodes,
                                                               TariffId tariffId, LocalDateTime startDate) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.TARIFF_EDIT);
            if (pedestalCodes == null || pedestalCodes.isEmpty()) {
                throw new InvalidInputException("No pedestal given");
            }
            List<Long> pedestalIds = new ArrayList<>();
            for (PedestalCode code : pedestalCodes) {
                Pedestal pedestal = pedestalService.findByCode(code.value());
                if (pedestal == null || pedestal.isDeleted()) {
                    throw ObjectNotFoundException.of("Pedestal", code);
                }
                pedestalService.checkIsManageable(principal, pedestal);
                pedestalIds.add(pedestal.getId());
            }
            checkTariffOrRemoval(principal, tariffId);
            return TariffMapper.toApplicabilityDTOs(tariffApplicabilityService.applyTariffToPedestals(pedestalIds,
                    tariffId != null ? tariffId.value() : null, startDate, principal.getUsername()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public TariffApplicabilityDTO applyTariffToSocket(Username authUsername, SocketId socketId, TariffId tariffId,
                                                      LocalDateTime startDate) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.TARIFF_EDIT);
            Socket socket = fetchSocket(socketId);
            pedestalService.checkIsManageable(principal, socket.getPedestal());
            checkTariffOrRemoval(principal, tariffId);
            return TariffMapper.toDTO(tariffApplicabilityService.applyTariffToSocket(socket.getId(),
                    tariffId != null ? tariffId.value() : null, startDate, principal.getUsername()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public List<TariffApplicabilityDTO> applyTariffToAffiliatePedestals(Username authUsername, Username ownerUsername,
                                                                        TariffId tariffId, LocalDateTime startDate) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.TARIFF_EDIT);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            userinfoService.checkIsManageable(principal, owner, true);
            fetchTariff(principal, tariffId);
            return TariffMapper.toApplicabilityDTOs(tariffApplicabilityService.applyTariffToAffiliatePedestals(owner,
                    tariffId.value(), startDate));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public TariffDTO setCampsiteDefaultTariff(Username authUsername, GroupId groupId, TariffId tariffId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.TARIFF_EDIT);
            Group group = groupService.fetchByGroupId(groupId.value());
            if (group == null) {
                throw ObjectNotFoundException.of("Group", groupId);
            }
            if (!GroupType.SITE.isGroupType(group)) {
                throw new InvalidInputException(String.format("Group %s is no SITE group", groupId));
            }
            if (!groupService.isManageable(principal.getGroup(GroupType.SITE), group)) {
                throw new AccessNotAllowedException(String.format("User '%s' has no access to group %s",
                        principal.getUsername(), groupId));
            }
            checkTariffOrRemoval(principal, tariffId);
            tariffApplicabilityService.setCampsiteDefaultTariff(group, tariffId != null ? tariffId.value() : null,
                    principal.getUsername());
            return TariffMapper.toDTO(defaultTariffService.findDefaultTariff(group.getId()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SocketTariffDTO findSocketTariff(Username authUsername, SocketId socketId, LocalDateTime at) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Socket socket = fetchSocketInScope(principal, socketId);
            LocalDateTime when = at != null ? at : LocalDateTime.now();
            ResolvedTariff resolved = null;
            try {
                resolved = tariffResolver.resolve(socket, null, when);
            } catch (NoTariffFoundException e) {
                // shown as a socket without tariff
            }
            return new SocketTariffDTO(socket.getId(), socket.getSocketCode(), socket.getPitchNo(), when,
                    resolved != null ? resolved.source().name() : null,
                    resolved != null ? TariffMapper.toDTO(resolved.tariff()) : null,
                    TariffMapper.toApplicabilityDTOs(tariffApplicabilityService.findAssignmentHistory(socket)));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PriceSimulationDTO simulatePrice(Username authUsername, SocketId socketId, TariffId tariffId,
                                            LocalDateTime plugIn, LocalDateTime plugOut, Kwh kwh, Integer nights) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Socket socket = fetchSocketInScope(principal, socketId);
            if (plugIn == null || plugOut == null) {
                throw new InvalidInputException("Plug-in and plug-out are needed");
            }
            if (plugOut.isBefore(plugIn)) {
                throw new InvalidInputException("Plug-out lies before plug-in");
            }
            if (kwh == null) {
                throw new InvalidInputException("Energy is missing");
            }
            if (nights != null && nights < 0) {
                throw new InvalidInputException("Nights must not be negative");
            }
            PricingInput input = PricingInput.of(socket.getId(), null, plugIn, plugOut, kwh.value(), nights);
            PriceCalculation calculation = tariffId != null
                    ? pricingService.calculateCost(fetchTariff(principal, tariffId), input)
                    : pricingService.calculateCost(input);
            return TariffMapper.toDTO(calculation);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private Tariff fetchTariff(Userinfo principal, TariffId tariffId) {
        if (tariffId == null) {
            throw new InvalidInputException("Tariff is missing");
        }
        Tariff tariff = tariffService.findTariffForUser(principal, tariffId.value());
        if (tariff == null) {
            throw ObjectNotFoundException.of("Tariff", tariffId);
        }
        return tariff;
    }

    private void checkTariffOrRemoval(Userinfo principal, TariffId tariffId) {
        if (tariffId != null) {
            fetchTariff(principal, tariffId);
        }
    }

    private Socket fetchSocket(SocketId socketId) {
        Socket socket = pedestalService.findSocket(socketId.value());
        if (socket == null || socket.isDeleted()) {
            throw ObjectNotFoundException.of("Socket", socketId);
        }
        return socket;
    }

    /**
     * A socket outside the scope is reported as unknown, like in the pedestal lookups.
     */
    private Socket fetchSocketInScope(Userinfo principal, SocketId socketId) {
        Socket socket = fetchSocket(socketId);
        if (!pedestalService.isManageable(principal, socket.getPedestal())) {
            throw ObjectNotFoundException.of("Socket", socketId);
        }
        return socket;
    }
}
