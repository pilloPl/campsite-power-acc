package io.pillopl.webservice.console.common;

/**
 * Paths of the console web services and of the pedestal link. Every operation is a POST of its request to {@code <service path>/<operation>}.
 */
public final class WebServicePaths {

    public static final String CONSOLE = "/api/console";

    /**
     * Login, logout and the logged-in user of the console.
     */
    public static final String AUTH = CONSOLE + "/auth";

    public static final String GROUP = CONSOLE + "/group";

    public static final String PEDESTAL = CONSOLE + "/pedestal";

    public static final String COMMISSIONING = CONSOLE + "/commissioning";

    public static final String USER = CONSOLE + "/user";

    public static final String ADMINISTRATION = CONSOLE + "/administration";

    public static final String CARD = CONSOLE + "/card";

    public static final String HOUSE_CARD = CONSOLE + "/houseCard";
    public static final String TARIFF = CONSOLE + "/tariff";

    public static final String CONSUMPTION_RECORD = CONSOLE + "/consumptionRecord";

    public static final String GUEST = CONSOLE + "/guest";

    public static final String ISSUER = CONSOLE + "/issuer";
    public static final String REPORT = CONSOLE + "/report";

    public static final String DASHBOARD = CONSOLE + "/dashboard";

    public static final String MAP = CONSOLE + "/map";

    public static final String SUPPLY = CONSOLE + "/supply";

    public static final String QR_PAY_CONSOLE = CONSOLE + "/qrPay";

    public static final String POWER_BUDGET = CONSOLE + "/powerBudget";

    public static final String MONITORING = CONSOLE + "/monitoring";

    public static final String ERP_BILLING = CONSOLE + "/erpBilling";

    public static final String JOB = CONSOLE + "/job";

    /**
     * Calls of the payment app (QR payment at the socket), not of the console.
     */
    public static final String QR_PAY = "/api/qrpay";

    /**
     * Messages of the pedestal controllers on link v1: {@code <path>/<pedestal code>/<message>}.
     */
    public static final String PEDESTAL_LINK = "/api/pedestal-link";

    public static final String PEDESTAL_LINK_V1 = PEDESTAL_LINK + "/v1";

    public static final String PEDESTAL_LINK_V2 = PEDESTAL_LINK + "/v2";

    public static final String PEDESTAL_LINK_V3 = PEDESTAL_LINK + "/v3";

    private WebServicePaths() {
        throw new AssertionError();
    }
}
