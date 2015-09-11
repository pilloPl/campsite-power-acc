package io.pillopl.qrpay.state;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The payment is finished; the archive job moves the chain away.
 */
@Component
@Scope("prototype")
public class QrPayClosedState extends QrPayState {
}
