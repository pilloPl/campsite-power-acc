package io.pillopl.webservice.console.common;

import io.pillopl.commons.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Base of the web services: holds the service interface bean and turns every exception of a call into the fault of
 * the web service, logging technical errors with their stack trace.
 *
 * @param <T> the service interface
 * @param <F> the fault of the web service
 */
public abstract class AbstractWebService<T, F extends WebServiceFault> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWebService.class);

    private final T bean;

    protected AbstractWebService(T bean) {
        this.bean = bean;
    }

    protected T getBean() {
        return bean;
    }

    protected abstract F createFault(String message, WsErrorCode errorCode);

    protected <R> R invoke(String operation, Supplier<R> call) throws F {
        try {
            return call.get();
        } catch (RuntimeException e) {
            WsErrorCode errorCode = FaultMapper.toErrorCode(e);
            if (errorCode == WsErrorCode.TECHNICAL_ERROR) {
                LOG.error(String.format("%s.%s failed: %s", getClass().getSimpleName(), operation,
                        ExceptionUtil.getStackTrace(e)));
            } else {
                LOG.info(String.format("%s.%s refused (%s): %s", getClass().getSimpleName(), operation, errorCode,
                        ExceptionUtil.getRootCauseMessage(e)));
            }
            throw createFault(FaultMapper.toMessage(e, errorCode), errorCode);
        }
    }
}
