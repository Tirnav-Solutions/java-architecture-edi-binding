package javax.edi.service.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Simple sliding-window rate limiter for enterprise protection.
 * Limits requests per IP per minute.
 * 
 * Configuration:
 *   edi.security.rate-limit-enabled=true
 *   edi.security.rate-limit-per-minute=100
 */
@Component
@Order(2)
public class RateLimitFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${edi.security.rate-limit-enabled:false}")
    private boolean rateLimitEnabled;

    @Value("${edi.security.rate-limit-per-minute:100}")
    private int maxRequestsPerMinute;

    // Simple per-IP counter with minute-based reset
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIp(httpRequest);

        WindowCounter counter = counters.computeIfAbsent(clientIp, k -> new WindowCounter());
        if (counter.incrementAndCheck(maxRequestsPerMinute)) {
            chain.doFilter(request, response);
        } else {
            LOG.warn("Rate limit exceeded for IP {}: {} requests/min", clientIp, maxRequestsPerMinute);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("Retry-After", "60");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. "
                    + "Maximum " + maxRequestsPerMinute + " requests per minute.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple minute-window counter. Resets when the minute changes.
     */
    private static class WindowCounter {
        private long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        synchronized boolean incrementAndCheck(int limit) {
            long now = System.currentTimeMillis();
            long currentMinute = now / 60_000;
            long windowMinute = windowStart / 60_000;

            if (currentMinute != windowMinute) {
                // New minute window -- reset
                windowStart = now;
                count.set(1);
                return true;
            }

            return count.incrementAndGet() <= limit;
        }
    }
}
