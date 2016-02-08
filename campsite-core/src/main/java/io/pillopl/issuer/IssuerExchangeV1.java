package io.pillopl.issuer;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Interface generation 1: semicolon separated files with a header line in the partner's exchange directory
 * ({@link IssuerPartner#getEndpoint()}), which is synchronised with the partner by SFTP outside the application.
 * Incoming files are read in name order and moved to {@code done} once read; outgoing files get a time stamp.
 * <pre>
 *   cards-in/      card list changes of the partner        cards-out/     card list changes of the network
 *   records-in/    records of the partner's campsites      records-out/   records of the partner's members
 * </pre>
 */
@Component
public class IssuerExchangeV1 implements IssuerExchange {

    private static final Logger LOG = LoggerFactory.getLogger(IssuerExchangeV1.class);

    static final String CARDS_IN = "cards-in";
    static final String CARDS_OUT = "cards-out";
    static final String RECORDS_IN = "records-in";
    static final String RECORDS_OUT = "records-out";
    static final String DONE = "done";

    static final String SEPARATOR = ";";

    static final String CARD_HEADER = "CARD_UID;CARD_NUMBER;ACTION;ACTION_DATE;BILLING_ACCOUNT_NO;VALID_TO";

    static final String RECORD_HEADER = "RECORD_NO;CAMPSITE;SOCKET_CODE;PITCH_NO;CARD_RFID;PLUG_IN;PLUG_OUT;"
            + "DURATION_S;NIGHTS;KWH;COST;CURRENCY;TARIFF";

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DateService dateService;

    public IssuerExchangeV1(DateService dateService) {
        this.dateService = dateService;
    }

    @Override
    public IssuerPartner.InterfaceVersion getInterfaceVersion() {
        return IssuerPartner.InterfaceVersion.V1;
    }

    @Override
    public List<CardLine> fetchCards(IssuerPartner partner) {
        return readIncoming(partner, CARDS_IN, CARD_HEADER, row -> new CardLine(cell(row, 0), cell(row, 1),
                cell(row, 2), cell(row, 3), cell(row, 4), parseDate(cell(row, 5))));
    }

    @Override
    public void sendCards(IssuerPartner partner, List<CardLine> cards) {
        List<String> lines = new ArrayList<>();
        lines.add(CARD_HEADER);
        for (CardLine card : cards) {
            lines.add(String.join(SEPARATOR, text(card.cardRfidHex()), text(card.cardNumber()),
                    text(card.actionCode()), text(card.actionDate()), text(card.billingAccountNo()),
                    card.validTo() != null ? card.validTo().toString() : ""));
        }
        writeOutgoing(partner, CARDS_OUT, "cards", lines);
    }

    @Override
    public void sendConsumptionRecords(IssuerPartner partner, List<ConsumptionRecordDTO> records) {
        List<String> lines = new ArrayList<>();
        lines.add(RECORD_HEADER);
        for (ConsumptionRecordDTO r : records) {
            lines.add(String.join(SEPARATOR, text(r.recordNo()), text(r.campsiteName()), text(r.socketCode()),
                    text(r.pitchNo()), text(r.cardRfid()), text(r.plugIn()), text(r.plugOut()),
                    text(r.durationSeconds()), String.valueOf(r.nights()), text(r.kwh()), text(r.cost()),
                    text(r.currency()), text(r.tariffName())));
        }
        writeOutgoing(partner, RECORDS_OUT, "records", lines);
    }

    @Override
    public List<ConsumptionRecordDTO> fetchConsumptionRecords(IssuerPartner partner) {
        return readIncoming(partner, RECORDS_IN, RECORD_HEADER, row -> new ConsumptionRecordDTO(null, cell(row, 0),
                null, cell(row, 2), cell(row, 3), null, cell(row, 1), null, null, cell(row, 4), null, null, null,
                true, false, false, parseDateTime(cell(row, 5)), parseDateTime(cell(row, 6)),
                parseLong(cell(row, 7)), parseInt(cell(row, 8)), parseDecimal(cell(row, 9)), null,
                parseDecimal(cell(row, 10)), cell(row, 11), null, null, cell(row, 12), null, null, null, null,
                null));
    }

    /**
     * The lines of every incoming file in name order. The files are moved to {@code done} only when all of them
     * could be read; an unreadable file fails the run and every file stays for the next one.
     */
    private <T> List<T> readIncoming(IssuerPartner partner, String directory, String header,
                                     Function<String[], T> toLine) {
        Path incoming = directory(partner).resolve(directory);
        List<T> result = new ArrayList<>();
        if (!Files.isDirectory(incoming)) {
            LOG.info(String.format("No directory %s for %s", incoming, partner.getName()));
            return result;
        }
        List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(incoming, "*.csv")) {
            stream.forEach(paths::add);
        } catch (IOException e) {
            throw new IssuerExchangeException("Cannot list " + incoming + ": " + e.getMessage(), e);
        }
        paths.sort(null);
        for (Path path : paths) {
            for (String[] row : readFile(path, header)) {
                result.add(toLine.apply(row));
            }
        }
        // all files read: none of them is read again
        for (Path path : paths) {
            moveToDone(path);
        }
        return result;
    }

    private static List<String[]> readFile(Path path, String header) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IssuerExchangeException("Cannot read " + path + ": " + e.getMessage(), e);
        }
        if (lines.isEmpty() || !header.equalsIgnoreCase(lines.get(0).strip())) {
            throw new IssuerExchangeException("File " + path.getFileName() + " has not the header " + header);
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                rows.add(lines.get(i).split(SEPARATOR, -1));
            }
        }
        LOG.info(String.format("%s lines read from %s", rows.size(), path));
        return rows;
    }

    private static void moveToDone(Path path) {
        try {
            Path done = Files.createDirectories(path.getParent().resolve(DONE));
            Files.move(path, done.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IssuerExchangeException("Cannot move " + path + " to " + DONE + ": " + e.getMessage(), e);
        }
    }

    private void writeOutgoing(IssuerPartner partner, String directory, String prefix, List<String> lines) {
        try {
            Path outgoing = Files.createDirectories(directory(partner).resolve(directory));
            Path file = outgoing.resolve(prefix + "-" + FILE_STAMP.format(dateService.getNow()) + ".csv");
            Files.write(file, lines, StandardCharsets.UTF_8);
            LOG.info(String.format("%s lines written to %s", lines.size() - 1, file));
        } catch (IOException e) {
            throw new IssuerExchangeException("Cannot write the " + prefix + " of " + partner.getName() + ": "
                    + e.getMessage(), e);
        }
    }

    private static Path directory(IssuerPartner partner) {
        if (partner.getEndpoint() == null || partner.getEndpoint().isBlank()) {
            throw new IssuerExchangeException("No exchange directory for " + partner.getName());
        }
        return Path.of(partner.getEndpoint());
    }

    private static String cell(String[] row, int index) {
        if (index >= row.length) {
            return null;
        }
        String value = row[index].strip();
        return value.isEmpty() ? null : value;
    }

    private static String text(Object value) {
        return value != null ? value.toString().replace(SEPARATOR, ",") : "";
    }

    private static LocalDate parseDate(String value) {
        try {
            return value != null ? LocalDate.parse(value) : null;
        } catch (DateTimeParseException e) {
            throw new IssuerExchangeException("Not a date: " + value, e);
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        try {
            return value != null ? LocalDateTime.parse(value) : null;
        } catch (DateTimeParseException e) {
            throw new IssuerExchangeException("Not a date and time: " + value, e);
        }
    }

    private static BigDecimal parseDecimal(String value) {
        try {
            return value != null ? new BigDecimal(value) : null;
        } catch (NumberFormatException e) {
            throw new IssuerExchangeException("Not a number: " + value, e);
        }
    }

    private static Long parseLong(String value) {
        try {
            return value != null ? Long.valueOf(value) : null;
        } catch (NumberFormatException e) {
            throw new IssuerExchangeException("Not a number: " + value, e);
        }
    }

    private static int parseInt(String value) {
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            throw new IssuerExchangeException("Not a number: " + value, e);
        }
    }
}
