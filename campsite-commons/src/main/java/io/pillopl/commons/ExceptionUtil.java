package io.pillopl.commons;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Static helpers for logging exceptions of the web service layer.
 */
public final class ExceptionUtil {

    private ExceptionUtil() {
        throw new AssertionError();
    }

    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer, true));
        return writer.toString();
    }

    /**
     * Message of the innermost cause, or its class name when it has no message.
     */
    public static String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
