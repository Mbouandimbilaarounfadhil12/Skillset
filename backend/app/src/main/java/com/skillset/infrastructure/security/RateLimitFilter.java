package com.skillset.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter: max 60 requests per IP per minute.
 * WebSocket handshake paths are excluded (they hold long connections).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int    MAX_REQUESTS  = 60;
    private static final long   WINDOW_MS     = 60_000L;

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws") || path.startsWith("/ws-sockjs");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String ip  = resolveIp(request);
        long   now = System.currentTimeMillis();

        Deque<Long> hits = windows.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (hits) {
            // Evict timestamps outside the current window
            while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW_MS) {
                hits.pollFirst();
            }
            if (hits.size() >= MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Trop de requêtes. Réessayez dans une minute.\"}");
                return;
            }
            hits.addLast(now);
        }

        chain.doFilter(request, response);
    }

    private String resolveIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
