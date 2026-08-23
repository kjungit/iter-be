package com.example.iter.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void suppliedRequestIdIsSharedWithResponseAndDownstreamLogs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/devices");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "client-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> downstreamRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                downstreamRequestId.set(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)));

        assertThat(downstreamRequestId).hasValue("client-request-123");
        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("client-request-123");
        assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void unsafeRequestIdIsReplacedAndPreviousMdcValueIsRestored() throws Exception {
        MDC.put(RequestLoggingFilter.REQUEST_ID_MDC_KEY, "outer-request");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rentals");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "invalid request id\nforged-log");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> generatedRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                generatedRequestId.set(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)));

        assertThat(generatedRequestId.get())
                .isNotEqualTo("invalid request id\nforged-log")
                .matches("[0-9a-f-]{36}");
        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo(generatedRequestId.get());
        assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isEqualTo("outer-request");
    }
}
