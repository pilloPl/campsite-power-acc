package io.pillopl.jobs;

import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every job changes or recalculates state (export status, sessions, card lists, views): a second instance must
 * never start while the first one still runs.
 */
class JobConcurrencyTest {

    @Test
    void everyJobRunsOneInstanceAtATime() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Job.class));
        List<String> jobClasses = scanner.findCandidateComponents("io.pillopl.jobs").stream()
                .map(BeanDefinition::getBeanClassName).toList();

        assertThat(jobClasses).hasSize(14);
        for (String jobClass : jobClasses) {
            assertThat(Class.forName(jobClass).isAnnotationPresent(DisallowConcurrentExecution.class))
                    .as(jobClass).isTrue();
        }
    }
}
