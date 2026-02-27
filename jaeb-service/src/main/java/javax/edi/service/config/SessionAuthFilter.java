package javax.edi.service.config;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Session-based authentication filter for the web UI (/ui/**).
 * Redirects unauthenticated users to the login page.
 * Login, signup, static assets, and API paths are exempt.
 */
@Component
@Order(3)
public class SessionAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        // Only protect /ui/** paths (except login/signup)
        if (!path.startsWith("/ui/") && !path.equals("/ui")) {
            chain.doFilter(request, response);
            return;
        }

        // Exempt login and signup pages
        if (path.equals("/ui/login") || path.equals("/ui/signup")) {
            chain.doFilter(request, response);
            return;
        }

        // Check session
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).sendRedirect("/ui/login");
        }
    }
}
