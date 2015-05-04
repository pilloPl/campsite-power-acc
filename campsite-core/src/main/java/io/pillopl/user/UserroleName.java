package io.pillopl.user;

/**
 * Names of the roles the code refers to (rows of USERROLE).
 */
public enum UserroleName {

    RECEPTION,
    CAMPSITE_MANAGER,
    MARKET_ADMIN,
    SERVICE_TECHNICIAN,
    CUSTOMER_SERVICE,

    /**
     * Owner of an independent (affiliate) campsite in the network, self-service console.
     */
    AFFILIATE_OWNER,

    /**
     * Guest with a seasonal contract on a fixed pitch, self-service console.
     */
    SEASONAL_RESIDENT,

    /**
     * Price reports for affiliate owners outside the home market.
     */
    AFFILIATE_PRICE_REPORTING,

    SELF_SERVICE_USER,

    SELF_SERVICE_SUPPORT,

    /**
     * Technical user of the payment app calling the QR payment web service.
     */
    QRPAY_APP_CLIENT
}
