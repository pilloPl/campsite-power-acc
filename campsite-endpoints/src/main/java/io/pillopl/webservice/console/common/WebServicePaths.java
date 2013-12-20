package io.pillopl.webservice.console.common;

/**
 * Paths of the console web services. Every operation is a POST of its request to {@code <service path>/<operation>}.
 */
public final class WebServicePaths {

    public static final String CONSOLE = "/api/console";

    public static final String GROUP = CONSOLE + "/group";

    public static final String PEDESTAL = CONSOLE + "/pedestal";

    public static final String COMMISSIONING = CONSOLE + "/commissioning";

    public static final String USER = CONSOLE + "/user";

    public static final String ADMINISTRATION = CONSOLE + "/administration";

    public static final String TARIFF = CONSOLE + "/tariff";

    private WebServicePaths() {
        throw new AssertionError();
    }
}
