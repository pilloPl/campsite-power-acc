package io.pillopl.issuer;

import io.pillopl.api.IssuerApi;
import io.pillopl.dto.admin.IssuerSyncStatusDTO;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Issuer exchange calls of the console (CONFIG_EDIT: the partners are network configuration).
 */
@Service
public class IssuerApiService implements IssuerApi {

    private static final Logger LOG = LoggerFactory.getLogger(IssuerApiService.class);

    private final IssuerExchangeService issuerExchangeService;

    private final IssuerPartnerDao issuerPartnerDao;

    private final PrincipalResolver principalResolver;

    private final TransactionTemplate readOnlyTransaction;

    public IssuerApiService(IssuerExchangeService issuerExchangeService, IssuerPartnerDao issuerPartnerDao,
                            PrincipalResolver principalResolver, PlatformTransactionManager transactionManager) {
        this.issuerExchangeService = issuerExchangeService;
        this.issuerPartnerDao = issuerPartnerDao;
        this.principalResolver = principalResolver;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    @Override
    public List<IssuerSyncStatusDTO> runIssuerExchange(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            LOG.info(String.format("Issuer exchange started by %s", principal.getUsername()));
            issuerExchangeService.exchangeAll();
            // the exchange commits per partner and direction: no transaction around it, one to read the result
            return readOnlyTransaction.execute(status -> findSyncStatuses());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private List<IssuerSyncStatusDTO> findSyncStatuses() {
        List<IssuerSyncStatusDTO> result = new ArrayList<>();
        for (IssuerPartner partner : issuerPartnerDao.findExchanging()) {
            List<IssuerSyncStatus> statuses = new ArrayList<>(partner.getSyncStatuses());
            statuses.sort(Comparator.comparing(IssuerSyncStatus::getDirection));
            for (IssuerSyncStatus status : statuses) {
                result.add(toDTO(partner, status));
            }
        }
        return result;
    }

    static IssuerSyncStatusDTO toDTO(IssuerPartner partner, IssuerSyncStatus s) {
        return new IssuerSyncStatusDTO(partner.getName(), partner.getInterfaceVersion().name(),
                s.getDirection().name(), s.getStatus().name(), s.getLastRun(), s.getLastSuccess(),
                s.getItemsAccepted(), s.getItemsRejected(), s.getMessage());
    }
}
