package io.pillopl.infrastructure;

import io.pillopl.api.PedestalApi;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.dto.infrastructure.PedestalDTO;
import io.pillopl.dto.infrastructure.PedestalSummaryDTO;
import io.pillopl.dto.infrastructure.SocketDTO;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.PitchNo;
import io.pillopl.value.SocketCode;
import io.pillopl.value.SocketId;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pedestal calls of the console on top of {@link PedestalService}. Viewing needs PEDESTAL_VIEW, changes PEDESTAL_EDIT;
 * the pedestal must lie in the SITE scope of the calling user.
 */
@Service
@Transactional
public class PedestalApiService implements PedestalApi {

    private final PedestalService pedestalService;

    private final PrincipalResolver principalResolver;

    public PedestalApiService(PedestalService pedestalService, PrincipalResolver principalResolver) {
        this.pedestalService = pedestalService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public ListPage<PedestalSummaryDTO> findPedestalsByState(Username authUsername, String state,
                                                             Pagination pagination) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            PedestalListState listState = ApiValues.parseEnumOrNull(PedestalListState.class, state, "pedestal state");
            PedestalPage page = pagination != null
                    ? pedestalService.findPedestalsByState(principal, listState, pagination.page(),
                    pagination.rowsPerPage())
                    : pedestalService.findPedestalsByState(principal, listState, null, null);
            return ListPage.of(PedestalMapper.toSummaries(page.pedestals()), page.total(), pagination);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PedestalDTO getPedestal(Username authUsername, PedestalCode code) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Pedestal pedestal = pedestalService.findByCodeForUser(principal, code.value());
            if (pedestal == null) {
                throw ObjectNotFoundException.of("Pedestal", code);
            }
            return PedestalMapper.toDTO(pedestal, pedestalService.findState(pedestal.getId()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PedestalDTO findPedestalBySocketCode(Username authUsername, SocketCode socketCode) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Socket socket = pedestalService.findSocketByCode(socketCode.value());
            if (socket == null || socket.isDeleted() || !pedestalService.isManageable(principal, socket.getPedestal())) {
                // a socket outside the scope is reported as unknown, like a pedestal outside the scope
                throw ObjectNotFoundException.of("Socket", socketCode);
            }
            Pedestal pedestal = socket.getPedestal();
            return PedestalMapper.toDTO(pedestal, pedestalService.findState(pedestal.getId()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public void deletePedestal(Username authUsername, Username ownerUsername, PedestalCode code) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            checkPedestalExists(code);
            pedestalService.deletePedestal(principal, owner, code.value());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public PedestalDTO updateLocation(Username authUsername, Username ownerUsername, PedestalCode code,
                                      LocationDTO location) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            checkPedestalExists(code);
            Pedestal pedestal = pedestalService.updateLocation(principal, owner, code.value(),
                    PedestalMapper.toLocation(location));
            return PedestalMapper.toDTO(pedestal, pedestalService.findState(pedestal.getId()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public SocketDTO assignPitch(Username authUsername, SocketId socketId, PitchNo pitchNo) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            checkSocketExists(socketId);
            return PedestalMapper.toDTO(pedestalService.assignPitch(principal, socketId.value(),
                    pitchNo != null ? pitchNo.value() : null));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public SocketDTO setFreeSocket(Username authUsername, SocketId socketId, boolean freeSocket) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            checkSocketExists(socketId);
            return PedestalMapper.toDTO(pedestalService.setFreeSocket(principal, socketId.value(), freeSocket));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private void checkPedestalExists(PedestalCode code) {
        if (pedestalService.findByCode(code.value()) == null) {
            throw ObjectNotFoundException.of("Pedestal", code);
        }
    }

    private void checkSocketExists(SocketId socketId) {
        Socket socket = pedestalService.findSocket(socketId.value());
        if (socket == null || socket.isDeleted()) {
            throw ObjectNotFoundException.of("Socket", socketId);
        }
    }
}
