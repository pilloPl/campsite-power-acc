package io.pillopl.commons;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

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
     * Message of the innermost cause, or its class name when it has no message. A cause chain that loops back
     * stops at the last exception before the loop.
     */
    public static String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable root = throwable;
        seen.add(root);
        while (root.getCause() != null && seen.add(root.getCause())) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
