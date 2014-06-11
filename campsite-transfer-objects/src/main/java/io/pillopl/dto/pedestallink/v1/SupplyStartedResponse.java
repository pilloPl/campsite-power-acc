package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Link v1: answer to a supply start. Transaction id 0 means refused (the socket must switch off); otherwise the
 * controller quotes the id when it stops, limits the current and shows price and greeting.
 */
public record SupplyStartedResponse(Integer transactionId, String status, LocalDate expiryDate, Integer maxCurrentA,
                                    String priceInformation, String greeting) implements Serializable {
}
