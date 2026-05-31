package io.github.cassio.ms_auth.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelatorFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlator";
    public static final String MDC_KEY = "correlator";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlator = request.getHeader(HEADER);
        boolean propagado = correlator != null && !correlator.isBlank();
        if (!propagado) {
            correlator = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlator);
        response.setHeader(HEADER, correlator);

        log.debug("→ {} {} [correlator={} {}]",
            request.getMethod(),
            request.getRequestURI(),
            correlator,
            propagado ? "propagado" : "gerado");

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.debug("← {} {} status={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus());
            MDC.remove(MDC_KEY);
        }
    }
}
