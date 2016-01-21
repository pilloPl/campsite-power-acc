package io.pillopl.erp;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.order.Order;
import io.pillopl.order.OrderService;
import io.pillopl.persistence.dao.OrderDao;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * File exchange with the ERP: the orders of new guests go out as partner requests and come back with the ERP's
 * partner and account numbers (or corrections of numbers); the billing lines go out monthly and come back booked or
 * refused. Files are semicolon separated with a header line, written under a temporary name and renamed when
 * complete; answer files are moved to {@code processed/} (or {@code error/} when a line could not be taken).
 * <p>
 * Not transactional itself: every order and every answer is taken in its own transaction of the services it calls,
 * so one bad line does not undo the others.
 */
@Service
public class ErpExchangeService {

    private static final Logger LOG = LoggerFactory.getLogger(ErpExchangeService.class);

    static final String SEPARATOR = ";";

    static final String PARTNER_REQUESTS = "partner-requests-";

    static final String PARTNER_ANSWERS = "partner-answers-";

    static final String BILLING_LINES = "billing-lines-";

    static final String BILLING_ANSWERS = "billing-answers-";

    static final String CSV = ".csv";

    static final String PROCESSED = "processed";

    static final String ERROR = "error";

    static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SystemPropertyService systemPropertyService;

    private final DateService dateService;

    private final OrderService orderService;

    private final OrderDao orderDao;

    private final ErpBillingDataService erpBillingDataService;

    public ErpExchangeService(SystemPropertyService systemPropertyService, DateService dateService,
                              OrderService orderService, OrderDao orderDao,
                              ErpBillingDataService erpBillingDataService) {
        this.systemPropertyService = systemPropertyService;
        this.dateService = dateService;
        this.orderService = orderService;
        this.orderDao = orderDao;
        this.erpBillingDataService = erpBillingDataService;
    }

    /**
     * The orders waiting for the ERP's numbers, each handed over once.
     *
     * @return number of orders written
     */
    public int exportPartnerRequests() {
        List<Order> orders = orderService.findOrdersAwaitingErpAccount(true);
        if (orders.isEmpty()) {
            return 0;
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.join(SEPARATOR, "ORDER_NUMBER", "GUEST_TYPE", "NAME", "FIRST_NAME", "ORGANISATION",
                "VAT_NUMBER", "STREET", "HOUSE_NO", "ZIP_CODE", "CITY", "COUNTRY", "LANGUAGE", "EMAIL", "PHONE",
                "IBAN", "PAYMENT_METHOD", "PARTNER_NO"));
        List<Long> orderIds = new ArrayList<>();
        for (Order order : orders) {
            lines.add(String.join(SEPARATOR, field(order.getOrderNumber()), field(order.getGuestType()),
                    field(order.getName()), field(order.getFirstName()), field(order.getOrganisationName()),
                    field(order.getVatNumber()), field(order.getStreet()), field(order.getHouseNo()),
                    field(order.getZipCode()), field(order.getCity()), field(order.getCountryIso()),
                    field(order.getLanguage()), field(order.getEmail()), field(order.getPhone()),
                    field(order.getIban()), field(order.getPaymentMethod()),
                    field(order.getGuest() != null ? order.getGuest().getErpPartnerNo() : null)));
            orderIds.add(order.getId());
        }
        Path file = write(PARTNER_REQUESTS, lines);
        int exported = orderService.markOrdersExported(orderIds);
        LOG.info(String.format("%s orders handed to the ERP in %s", exported, file));
        return exported;
    }

    /**
     * The ERP's partner answers: {@code ORDER;<order number>;<partner no>;<account no>} activates the order,
     * {@code CORRECTION;<old partner>;<old account>;<new partner>;<new account>} corrects numbers.
     *
     * @return number of answer lines taken
     */
    public int importPartnerAnswers() {
        int taken = 0;
        for (Path file : answerFiles(PARTNER_ANSWERS)) {
            boolean failed = false;
            for (String[] fields : read(file)) {
                try {
                    if (fields.length == 4 && "ORDER".equals(fields[0])) {
                        Order order = orderDao.findByOrderNumber(fields[1]);
                        if (order == null) {
                            throw new IllegalArgumentException("Order " + fields[1] + " not found");
                        }
                        orderService.enrichOrderWithErpNumbers(order.getId(), fields[2], fields[3]);
                    } else if (fields.length == 5 && "CORRECTION".equals(fields[0])) {
                        orderService.correctErpNumbers(fields[1], fields[2], fields[3], fields[4]);
                    } else {
                        throw new IllegalArgumentException("Unknown answer " + String.join(SEPARATOR, fields));
                    }
                    taken++;
                } catch (RuntimeException e) {
                    LOG.error(String.format("ERP partner answer in %s not taken: %s", file.getFileName(),
                            e.getMessage()));
                    failed = true;
                }
            }
            moveAway(file, failed);
        }
        return taken;
    }

    /**
     * The billing run of the month before (once) and the lines not yet sent (new and sent again) into one file.
     *
     * @return number of lines written
     */
    public int exportBillingLines() {
        erpBillingDataService.createErpBillingLinesOfLastMonth();
        List<ErpBillingLine> billingLines = erpBillingDataService.findLinesToTransfer();
        if (billingLines.isEmpty()) {
            return 0;
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.join(SEPARATOR, "PERIOD", "PARTNER_NO", "ACCOUNT_NO", "ORDER_NUMBER", "ARTICLE_NO",
                "DESCRIPTION", "QUANTITY", "UNIT_PRICE", "AMOUNT", "CURRENCY", "TAX_CLASS", "CLASSIFICATION",
                "SALES_OFFICE", "SALES_GROUP", "ASSIGNEE", "ORDER_TYPE", "TEXT"));
        for (ErpBillingLine line : billingLines) {
            lines.add(String.join(SEPARATOR, field(line.getBillingPeriod()), field(line.getErpPartnerNo()),
                    field(line.getBillingAccountNo()), field(line.getOrderNumber()), field(line.getArticleNo()),
                    field(line.getDescription()), field(line.getQuantity().toPlainString()),
                    field(line.getUnitPrice().toPlainString()), field(line.getAmount().toPlainString()),
                    field(line.getCurrency()), field(line.getTaxClass()), field(line.getArticleClassification()),
                    field(line.getSalesOffice()), field(line.getSalesGroup()), field(line.getAssignee()),
                    field(line.getOrderType()), field(line.getAlternateText())));
        }
        Path file = write(BILLING_LINES, lines);
        erpBillingDataService.markLinesTransferred(billingLines);
        LOG.info(String.format("%s ERP billing lines written to %s", billingLines.size(), file));
        return billingLines.size();
    }

    /**
     * The ERP's billing answers: {@code <period>;<account no>;<article no>;OK|ERROR;<text>}.
     *
     * @return number of answer lines taken
     */
    public int importBillingAnswers() {
        int taken = 0;
        for (Path file : answerFiles(BILLING_ANSWERS)) {
            boolean failed = false;
            for (String[] fields : read(file)) {
                try {
                    if (fields.length < 4 || !("OK".equals(fields[3]) || "ERROR".equals(fields[3]))) {
                        throw new IllegalArgumentException("Unknown answer " + String.join(SEPARATOR, fields));
                    }
                    if (!erpBillingDataService.takeErpAnswer(fields[0], fields[1], fields[2], "OK".equals(fields[3]),
                            fields.length > 4 ? fields[4] : null)) {
                        throw new IllegalArgumentException("No line sent for " + String.join(SEPARATOR, fields));
                    }
                    taken++;
                } catch (RuntimeException e) {
                    LOG.error(String.format("ERP billing answer in %s not taken: %s", file.getFileName(),
                            e.getMessage()));
                    failed = true;
                }
            }
            moveAway(file, failed);
        }
        return taken;
    }

    private Path write(String prefix, List<String> lines) {
        Path directory = Paths.get(systemPropertyService.getErpOutboundDirectory());
        String name = prefix + dateService.getNow().format(FILE_TIME) + CSV;
        try {
            Files.createDirectories(directory);
            Path temporary = directory.resolve(name + ".tmp");
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            return Files.move(temporary, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("ERP file " + name + " not written to " + directory, e);
        }
    }

    private List<Path> answerFiles(String prefix) {
        Path directory = Paths.get(systemPropertyService.getErpInboundDirectory());
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, prefix + "*" + CSV)) {
            for (Path file : stream) {
                files.add(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("ERP answers not read from " + directory, e);
        }
        files.sort(null);
        return files;
    }

    /**
     * @return the fields of the lines, without the header line and blank lines
     */
    private static List<String[]> read(Path file) {
        List<String[]> rows = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                if (!lines.get(i).isBlank()) {
                    rows.add(lines.get(i).trim().split(SEPARATOR, -1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("ERP answer file " + file + " not read", e);
        }
        return rows;
    }

    private static void moveAway(Path file, boolean failed) {
        Path target = file.resolveSibling(failed ? ERROR : PROCESSED);
        try {
            Files.createDirectories(target);
            Files.move(file, target.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("ERP answer file " + file + " not moved to " + target, e);
        }
    }

    /**
     * A value of a line: empty for null, the separator and line breaks never inside.
     */
    static String field(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().replace(SEPARATOR, ",").replace('\n', ' ').replace('\r', ' ');
    }
}
