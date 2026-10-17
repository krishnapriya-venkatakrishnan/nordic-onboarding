package dev.krishnapriya.onboarding.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Marks requests from the hourly synthetic sign-up, so their log lines (and the onboarding events
 * in them) carry synthetic=true and dashboards and alerts can leave them out. The domain never
 * sees this: it lives in the log context for the length of the request.
 *
 * Anyone could send the header; that only hides their own traffic from the charts, which is
 * acceptable for a demo.
 */
@Component
public class SyntheticTrafficFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Synthetic-Check";
    static final String FIELD = "synthetic";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"true".equalsIgnoreCase(request.getHeader(HEADER))) {
            chain.doFilter(request, response);
            return;
        }
        MDC.put(FIELD, "true");
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(FIELD);
        }
    }
}
