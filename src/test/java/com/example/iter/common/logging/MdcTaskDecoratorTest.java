package com.example.iter.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator taskDecorator = new MdcTaskDecorator();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void submittingContextIsAvailableDuringTaskAndWorkerContextIsRestored() {
        MDC.put(RequestLoggingFilter.REQUEST_ID_MDC_KEY, "request-123");
        AtomicReference<String> requestIdDuringTask = new AtomicReference<>();
        Runnable decorated = taskDecorator.decorate(() ->
                requestIdDuringTask.set(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)));

        MDC.put(RequestLoggingFilter.REQUEST_ID_MDC_KEY, "worker-before-task");
        decorated.run();

        assertThat(requestIdDuringTask).hasValue("request-123");
        assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isEqualTo("worker-before-task");
    }

    @Test
    void emptySubmittingContextDoesNotLeakPreviousRequestId() {
        Runnable decorated = taskDecorator.decorate(() ->
                assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull());

        MDC.put(RequestLoggingFilter.REQUEST_ID_MDC_KEY, "stale-worker-request");
        decorated.run();

        assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isEqualTo("stale-worker-request");
    }
}
