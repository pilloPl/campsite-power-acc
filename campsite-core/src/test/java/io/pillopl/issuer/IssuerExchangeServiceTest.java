package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.RfidConverter;
import io.pillopl.issuer.IssuerExchange.CardLine;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuerExchangeServiceTest {

    @Test
    void networkCardGoesToThePartnersAsHexUidWithoutItsBillingAccount() {
        CampingCard card = new CampingCard.Builder("3054198966", "HR-123").validTo(LocalDate.of(2026, 7, 14))
                .billingAccountNo("400012345678").build();
        card.calculatePartnerSyncState(PartnerSyncState.ADD);

        CardLine line = IssuerExchangeService.toCardLine(card, LocalDateTime.of(2026, 7, 11, 2, 30));

        assertThat(line).isEqualTo(new CardLine(RfidConverter.decimalToHexBigEndian("3054198966"), "HR-123", "ADD",
                "2026-07-11 02:30", null, LocalDate.of(2026, 7, 14)));
    }

    @Test
    void partnerOfAnInterfaceVersionWithoutTransportCannotBeExchangedWith() {
        IssuerExchangeService service = new IssuerExchangeService(null, null, null, null, null, null, null, null,
                null, List.of(new IssuerExchangeV1(null)), new NoTransactions());
        IssuerPartner partner = new IssuerPartner("CLUB", null, IssuerPartner.InterfaceVersion.V2, "http://x");

        assertThatThrownBy(() -> service.getExchange(partner)).isInstanceOf(IssuerExchangeException.class)
                .hasMessage("No exchange for interface version V2 of CLUB");
        partner.setInterfaceVersion(IssuerPartner.InterfaceVersion.V1);
        assertThat(service.getExchange(partner)).isInstanceOf(IssuerExchangeV1.class);
    }

    @Test
    void secondTransportForTheSameInterfaceVersionIsRefused() {
        assertThatThrownBy(() -> new IssuerExchangeService(null, null, null, null, null, null, null, null, null,
                List.of(new IssuerExchangeV1(null), new IssuerExchangeV1(null)), new NoTransactions()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Two exchanges for interface version V1: IssuerExchangeV1 and IssuerExchangeV1");
    }

    static class NoTransactions extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
