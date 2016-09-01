package io.pillopl.consumption;

import io.pillopl.api.ConsumptionRecordApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consumption record calls of the console: REPORT_VIEW to list the records of the caller's SITE and ISSUER scope,
 * CONSUMPTION_RECORD_REVIEW to confirm or reject a record of that scope waiting for review.
 */
@Service
@Transactional
public class ConsumptionRecordApiService implements ConsumptionRecordApi {

    private final ConsumptionRecordDao consumptionRecordDao;

    private final ConsumptionRecordWriteService consumptionRecordWriteService;

    private final GroupService groupService;

    private final PrincipalResolver principalResolver;

    public ConsumptionRecordApiService(ConsumptionRecordDao consumptionRecordDao,
                                       ConsumptionRecordWriteService consumptionRecordWriteService,
                                       GroupService groupService, PrincipalResolver principalResolver) {
        this.consumptionRecordDao = consumptionRecordDao;
        this.consumptionRecordWriteService = consumptionRecordWriteService;
        this.groupService = groupService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsumptionRecordDTO> findConsumptionRecords(Username authUsername, DateRange period,
                                                             String validationState, Pagination pagination) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            if (period == null || period.from() == null || period.to() == null) {
                throw new InvalidInputException("Period of the consumption records is missing");
            }
            RecordValidationState state = ApiValues.parseEnumOrNull(RecordValidationState.class, validationState,
                    "validation state");
            Long siteGroupId = groupIdOf(principal, GroupType.SITE);
            Long issuerGroupId = groupIdOf(principal, GroupType.ISSUER);
            List<ConsumptionRecord> records;
            if (pagination == null && state == null) {
                records = consumptionRecordDao.findConsumptionRecords(period.from(), period.to(), siteGroupId,
                        issuerGroupId);
            } else {
                Pagination page = pagination != null ? pagination : new Pagination(0, Pagination.MAX_ROWS_PER_PAGE);
                records = consumptionRecordDao.findConsumptionRecords(period.from(), period.to(), siteGroupId,
                        issuerGroupId, state, page.page() + 1, page.rowsPerPage());
            }
            return records.stream().map(ConsumptionRecordApiService::toDTO).toList();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public ConsumptionRecordDTO reviewConsumptionRecord(Username authUsername, Long recordId, boolean confirmed,
                                                        String comment) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONSUMPTION_RECORD_REVIEW);
            if (recordId == null) {
                throw new InvalidInputException("Consumption record id is missing");
            }
            ConsumptionRecord record = consumptionRecordDao.findById(recordId)
                    .orElseThrow(() -> ObjectNotFoundException.of("Consumption record", recordId));
            if (!isInScope(principal, record)) {
                throw new AccessNotAllowedException("Consumption record " + record.getRecordNo()
                        + " is not in the scope of " + principal.getUsername());
            }
            ConsumptionRecord reviewed = confirmed
                    ? consumptionRecordWriteService.confirmRecord(recordId, principal.getUsername(), comment)
                    : consumptionRecordWriteService.rejectRecord(recordId, principal.getUsername(), comment);
            return toDTO(reviewed);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public ConsumptionRecordDTO repriceConsumptionRecord(Username authUsername, Long recordId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONSUMPTION_RECORD_REVIEW);
            checkRecordInScope(principal, recordId);
            return toDTO(consumptionRecordWriteService.repriceRecord(recordId, principal.getUsername()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public ConsumptionRecordDTO requeueConsumptionRecordExport(Username authUsername, Long recordId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.BILLING_EDIT);
            checkRecordInScope(principal, recordId);
            return toDTO(consumptionRecordWriteService.requeueExport(recordId, principal.getUsername()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private void checkRecordInScope(Userinfo principal, Long recordId) {
        if (recordId == null) {
            throw new InvalidInputException("Consumption record id is missing");
        }
        ConsumptionRecord record = consumptionRecordDao.findById(recordId)
                .orElseThrow(() -> ObjectNotFoundException.of("Consumption record", recordId));
        if (!isInScope(principal, record)) {
            throw new AccessNotAllowedException("Consumption record " + record.getRecordNo()
                    + " is not in the scope of " + principal.getUsername());
        }
    }

    /**
     * The same two sides as the visibility SQL: the record's pedestal in the user's SITE subtree or its guest in
     * the user's ISSUER subtree.
     */
    private boolean isInScope(Userinfo principal, ConsumptionRecord record) {
        Group siteGroup = principal.getGroup(GroupType.SITE);
        if (siteGroup != null && record.getSiteGroupId() != null
                && groupService.findManageable(siteGroup).contains(record.getSiteGroupId())) {
            return true;
        }
        Group issuerGroup = principal.getGroup(GroupType.ISSUER);
        return issuerGroup != null && record.getIssuerGroupId() != null
                && groupService.findManageable(issuerGroup).contains(record.getIssuerGroupId());
    }

    private static Long groupIdOf(Userinfo principal, GroupType type) {
        Group group = principal.getGroup(type);
        return group != null ? group.getId() : null;
    }

    public static ConsumptionRecordDTO toDTO(ConsumptionRecord r) {
        SettlementFlags flags = r.getSettlementFlags();
        return new ConsumptionRecordDTO(r.getId(), r.getRecordNo(), r.getPedestalCode(), r.getSocketCode(),
                r.getPitchNo(), r.getCampsiteCode(), r.getCampsiteName(), r.getSiteTenant(), r.getIssuerTenant(),
                r.getCardRfid(), r.getCardNumber(), r.getGuestName(), name(r.getPaymentChannel()),
                flags != null && flags.isBillable(), flags != null && flags.isSettledByCampsite(),
                flags != null && flags.isHouseCardRental(), r.getPlugIn(), r.getPlugOut(), r.getDurationSeconds(),
                r.getNights(), r.getKwh(), name(r.getStopReason()), r.getCost(), r.getCurrency(),
                name(r.getPricingStatus()), r.getPricingFailureReason(), r.getTariffName(),
                name(r.getValidationState()), r.getSuspectReason(), name(r.getExportStatus()), r.getReviewedBy(),
                r.getReviewDate());
    }

    private static String name(Enum<?> value) {
        return value != null ? value.name() : null;
    }
}
