package io.pillopl.infrastructure;

import java.util.Locale;

/**
 * Tells the affiliate owner that a pedestal went back to the supplier. The mail implementation comes with the
 * mail templates (monitoring and mails); until then the default bean only logs.
 */
public interface DecommissioningNotifier {

    void pedestalBackToSupplier(Locale locale, String pedestalCode, String email);
}
