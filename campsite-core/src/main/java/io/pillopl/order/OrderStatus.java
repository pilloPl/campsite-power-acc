package io.pillopl.order;

/**
 * Where an order stands. A guest unknown to the ERP waits for the partner and account numbers; with both the order
 * is activated (contract, account and cards). Only orders not active yet can be cancelled.
 */
public enum OrderStatus {
    NEW,
    AWAITING_ERP_ACCOUNT,
    ACTIVE,
    CANCELLED;

    public boolean isOpen() {
        return this == NEW || this == AWAITING_ERP_ACCOUNT;
    }
}
