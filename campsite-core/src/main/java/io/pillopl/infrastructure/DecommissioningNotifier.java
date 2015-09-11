package io.pillopl.infrastructure;

import java.util.Locale;

/**
 * Tells the affiliate owner that a pedestal went back to the supplier. Implemented by the pedestal mails
 * ({@code io.pillopl.monitoring.PedestalMailService}).
 */
public interface DecommissioningNotifier {

    void pedestalBackToSupplier(Locale locale, String pedestalCode, String email);
}
