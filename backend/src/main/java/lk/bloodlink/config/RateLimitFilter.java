package lk.bloodlink.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiny fixed-window limiter (per IP) on search + auth endpoints.
 * Stops people scraping the donor list and brute-forcing logins.
 * For multiple server instances replace with Redis / API gateway limits.
 */
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, long[]> hits = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return !(p.startsWith("/api/search") || p.startsWith("/api/auth"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        boolean auth = req.getRequestURI().startsWith("/api/auth");
        int limit = auth ? 20 : 60;
        long now = System.currentTimeMillis();
        String key = req.getRemoteAddr() + (auth ? "|A" : "|S");
        long[] w = hits.compute(key, (k, v) -> {
            if (v == null || now - v[0] > 60_000) return new long[]{now, 1};
            v[1]++;
            return v;
        });
        if (hits.size() > 50_000) hits.clear();
        if (w[1] > limit) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"message\":\"Too many requests. Please wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
