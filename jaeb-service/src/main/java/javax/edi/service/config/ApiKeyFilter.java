package javax.edi.service.config;

import java.io.IOException;

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
 * API Key authentication filter for enterprise use.
 * 
 * When enabled, all /api/edi/** requests must include an API key
 * via the X-API-Key header.
 * 
 * Configuration in application.properties:
 *   edi.security.api-key-enabled=true
 *   edi.security.api-key=your-secret-key
 * 
 * Endpoints exempt from auth: /api/edi/health, /api/edi/info, /swagger-ui/**, /api-docs/**
 */
@Component
@Order(1)
public class ApiKeyFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyFilter.class);

    @Value("${edi.security.api-key-enabled:false}")
    private boolean apiKeyEnabled;

    @Value("${edi.security.api-key:}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!apiKeyEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Exempt paths -- health, info, swagger, actuator
        if (isExempt(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Only protect /api/** paths
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = httpRequest.getHeader("X-API-Key");
        if (providedKey == null || providedKey.isEmpty()) {
            providedKey = httpRequest.getParameter("apiKey");
        }

        if (apiKey.equals(providedKey)) {
            chain.doFilter(request, response);
        } else {
            LOG.warn("Unauthorized API access attempt from {} to {}", httpRequest.getRemoteAddr(), path);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: Invalid or missing API key. "
                    + "Include X-API-Key header.\"}");
        }
    }

    private boolean isExempt(String path) {
        return path.equals("/api/edi/health")
            || path.equals("/api/edi/info")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/api-docs")
            || path.startsWith("/actuator")
            || path.startsWith("/h2-console")
            || path.equals("/favicon.ico");
    }
}
