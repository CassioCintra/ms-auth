package io.github.cassio.ms_auth.adapter.in.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelatorFilterTest {

    CorrelatorFilter sut;

    @BeforeEach
    void setUp() {
        sut = new CorrelatorFilter();
    }

    @Test
    void shouldGenerateCorrelatorWhenHeaderIsAbsent() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        sut.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelatorFilter.HEADER)).isNotBlank();
    }

    @Test
    void shouldReuseCorrelatorFromIncomingHeader() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelatorFilter.HEADER, "my-trace-id");
        var response = new MockHttpServletResponse();

        sut.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelatorFilter.HEADER)).isEqualTo("my-trace-id");
    }

    @Test
    void shouldClearMdcAfterRequest() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        sut.doFilterInternal(request, response, new MockFilterChain());

        assertThat(MDC.get(CorrelatorFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldPutCorrelatorInMdcDuringRequest() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelatorFilter.HEADER, "trace-123");
        var capturedMdcValue = new String[1];

        sut.doFilterInternal(request, new MockHttpServletResponse(), (req, res) ->
            capturedMdcValue[0] = MDC.get(CorrelatorFilter.MDC_KEY));

        assertThat(capturedMdcValue[0]).isEqualTo("trace-123");
    }
}
