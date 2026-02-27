package javax.edi.service.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Request/response audit logging interceptor.
 * Logs method, URI, client IP, response status, and processing time
 * for every API request. Essential for enterprise compliance and debugging.
 * 
 * Enable with:
 *   edi.audit.logging-enabled=true
 */
@Component
public class AuditLoggingConfig implements WebMvcConfigurer {

    @Value("${edi.audit.logging-enabled:true}")
    private boolean loggingEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (loggingEnabled) {
            registry.addInterceptor(new AuditInterceptor())
                    .addPathPatterns("/api/**");
        }
    }

    static class AuditInterceptor implements HandlerInterceptor {

        private static final Logger AUDIT = LoggerFactory.getLogger("EDI_AUDIT");
        private static final String START_TIME = "auditStartTime";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            request.setAttribute(START_TIME, System.currentTimeMillis());

            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null) clientIp = request.getRemoteAddr();

            String apiKey = request.getHeader("X-API-Key");
            String keyInfo = (apiKey != null && !apiKey.isEmpty())
                    ? " key=***" + apiKey.substring(Math.max(0, apiKey.length() - 4))
                    : " key=none";

            AUDIT.info(">> {} {} from={}{} content-length={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    clientIp,
                    keyInfo,
                    request.getContentLengthLong());

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            Long startTime = (Long) request.getAttribute(START_TIME);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;

            String status = ex != null ? "ERROR" : String.valueOf(response.getStatus());

            AUDIT.info("<< {} {} status={} time={}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration);
        }
    }
}
