package io.pillopl.event;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Executors of the event listeners.
 */
@Component
public class EventTaskExecutorFactory {

    static final int DEFAULT_QUEUE_CAPACITY = 10000;

    /**
     * One thread: the events of a listener are handled in the order they arrive.
     */
    public AsyncTaskExecutor singletonThreadPoolTaskExecutor(String name) {
        return threadPoolTaskExecutor(name, 1, 1);
    }

    public AsyncTaskExecutor threadPoolTaskExecutor(String name, int corePoolSize, int maxPoolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(name + "-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(DEFAULT_QUEUE_CAPACITY);
        executor.initialize();
        return executor;
    }
}
