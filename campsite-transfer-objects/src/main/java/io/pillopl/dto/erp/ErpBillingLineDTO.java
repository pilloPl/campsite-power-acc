package io.pillopl.dto.erp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A line of an ERP bill (id null in a preview): the account, the article, quantity times unit price, the export
 * state towards the ERP and the ERP's refusal text.
 */
public record ErpBillingLineDTO(Long id, String billingPeriod, String billingAccountNo, String erpPartnerNo,
                                String orderNumber, String lineType, String articleNo, String description,
                                BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, String currency,
                                String exportStatus, String errorText, LocalDateTime createDate,
                                LocalDateTime exportDate, LocalDateTime answerDate) implements Serializable {
}
