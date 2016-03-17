package io.pillopl.issuer;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.issuer.IssuerExchange.CardLine;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuerExchangeV1Test {

    @TempDir
    Path exchangeDirectory;

    IssuerExchangeV1 exchange;
    IssuerPartner partner;

    @BeforeEach
    void setUp() {
        DateService dateService = new DateService();
        dateService.setClock(Clock.fixed(LocalDateTime.of(2026, 7, 11, 2, 30).atZone(ZoneId.systemDefault())
                .toInstant(), ZoneId.systemDefault()));
        exchange = new IssuerExchangeV1(dateService);
        partner = new IssuerPartner("CARAVAN CLUB", null, IssuerPartner.InterfaceVersion.V1,
                exchangeDirectory.toString());
    }

    @Test
    void cardFilesOfThePartnerAreReadInNameOrderAndMovedToDone() throws IOException {
        incoming("cards-in", "b.csv", IssuerExchangeV1.CARD_HEADER, "04A1B2C4;CC-2;DELETE;2026-07-02 10:00;;");
        incoming("cards-in", "a.csv", IssuerExchangeV1.CARD_HEADER,
                "04A1B2C3;CC-1;ADD;2026-07-01 09:30;770012345678;2026-12-31", "");

        List<CardLine> cards = exchange.fetchCards(partner);

        assertThat(cards).containsExactly(
                new CardLine("04A1B2C3", "CC-1", "ADD", "2026-07-01 09:30", "770012345678", LocalDate.of(2026, 12, 31)),
                new CardLine("04A1B2C4", "CC-2", "DELETE", "2026-07-02 10:00", null, null));
        assertThat(exchangeDirectory.resolve("cards-in/done/a.csv")).exists();
        assertThat(exchangeDirectory.resolve("cards-in/a.csv")).doesNotExist();
        assertThat(exchange.fetchCards(partner)).isEmpty();
    }

    @Test
    void partnerWithoutIncomingDirectoryHasNothingToImport() {
        assertThat(exchange.fetchCards(partner)).isEmpty();
        assertThat(exchange.fetchConsumptionRecords(partner)).isEmpty();
    }

    @Test
    void fileWithAnotherHeaderFailsTheRunAndStays() throws IOException {
        incoming("cards-in", "a.csv", IssuerExchangeV1.CARD_HEADER, "04A1B2C3;CC-1;ADD;;770012345678;");
        incoming("cards-in", "b.csv", "UID,NUMBER", "04A1B2C3,CC-1");

        assertThatThrownBy(() -> exchange.fetchCards(partner)).isInstanceOf(IssuerExchangeException.class)
                .hasMessageContaining("b.csv has not the header");
        assertThat(exchangeDirectory.resolve("cards-in/a.csv")).exists();
        assertThat(exchangeDirectory.resolve("cards-in/b.csv")).exists();
    }

    @Test
    void networkCardListIsWrittenWithATimeStamp() throws IOException {
        exchange.sendCards(partner, List.of(new CardLine("0A0B0C0D", "HR-1", "ADD", "2026-07-11 02:30", null,
                LocalDate.of(2026, 7, 14))));

        Path file = exchangeDirectory.resolve("cards-out/cards-20260711023000.csv");
        assertThat(Files.readAllLines(file)).containsExactly(IssuerExchangeV1.CARD_HEADER,
                "0A0B0C0D;HR-1;ADD;2026-07-11 02:30;;2026-07-14");
    }

    @Test
    void recordsAreWrittenAndReadInTheSameColumns() throws IOException {
        ConsumptionRecordDTO record = new ConsumptionRecordDTO(1L, "BAY-20260710-000001", "HR-BAY-P001",
                "BAY-P001-1", "12", "BAY", "Bay; beach", null, null, "3054198966", null, null, "CARD", true, false,
                false, LocalDateTime.of(2026, 7, 9, 18, 0), LocalDateTime.of(2026, 7, 10, 9, 0), 54000L, 1,
                new BigDecimal("12.5000"), null, new BigDecimal("4.38"), "EUR", "PRICED", null, "Summer", "VALID",
                null, "NEW", null, null);

        exchange.sendConsumptionRecords(partner, List.of(record));
        Path written = exchangeDirectory.resolve("records-out/records-20260711023000.csv");
        Files.createDirectories(exchangeDirectory.resolve("records-in"));
        Files.copy(written, exchangeDirectory.resolve("records-in/records.csv"));

        List<ConsumptionRecordDTO> read = exchange.fetchConsumptionRecords(partner);

        assertThat(read).hasSize(1);
        ConsumptionRecordDTO line = read.get(0);
        assertThat(line.recordNo()).isEqualTo("BAY-20260710-000001");
        assertThat(line.campsiteName()).isEqualTo("Bay, beach");
        assertThat(line.socketCode()).isEqualTo("BAY-P001-1");
        assertThat(line.pitchNo()).isEqualTo("12");
        assertThat(line.cardRfid()).isEqualTo("3054198966");
        assertThat(line.plugIn()).isEqualTo(LocalDateTime.of(2026, 7, 9, 18, 0));
        assertThat(line.plugOut()).isEqualTo(LocalDateTime.of(2026, 7, 10, 9, 0));
        assertThat(line.durationSeconds()).isEqualTo(54000L);
        assertThat(line.nights()).isEqualTo(1);
        assertThat(line.kwh()).isEqualByComparingTo("12.5");
        assertThat(line.cost()).isEqualByComparingTo("4.38");
        assertThat(line.currency()).isEqualTo("EUR");
        assertThat(line.tariffName()).isEqualTo("Summer");
    }

    @Test
    void unreadableValueInARecordFileFailsTheRun() throws IOException {
        incoming("records-in", "r.csv", IssuerExchangeV1.RECORD_HEADER,
                "CLUB-1;Club;CLUB-P001-1;;8101;yesterday;2026-07-10T09:00;;1;3;1.00;EUR;");

        assertThatThrownBy(() -> exchange.fetchConsumptionRecords(partner))
                .isInstanceOf(IssuerExchangeException.class).hasMessage("Not a date and time: yesterday");
        assertThat(exchangeDirectory.resolve("records-in/r.csv")).exists();
    }

    @Test
    void partnerWithoutDirectoryCannotExchange() {
        partner.setEndpoint(" ");

        assertThatThrownBy(() -> exchange.sendCards(partner, List.of()))
                .hasMessage("No exchange directory for CARAVAN CLUB");
    }

    private void incoming(String directory, String name, String... lines) throws IOException {
        Path dir = Files.createDirectories(exchangeDirectory.resolve(directory));
        Files.write(dir.resolve(name), List.of(lines), StandardCharsets.UTF_8);
    }
}
