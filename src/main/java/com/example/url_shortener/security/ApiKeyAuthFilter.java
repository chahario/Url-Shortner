package com.example.url_shortener.security;

import com.example.url_shortener.domain.Client;
import com.example.url_shortener.repository.ClientRepository;
import com.example.url_shortener.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ClientRepository clientRepository;
    private final RateLimiterService rateLimiterService;

    public ApiKeyAuthFilter(ClientRepository clientRepository,
                            RateLimiterService rateLimiterService) {
        this.clientRepository = clientRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Public paths: the redirect (root), health, and (later) registration.
        // Everything under /api/** requires a key.
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                unauthorized(response, "Missing or malformed Authorization header");
                return;
            }

            String apikey = header.substring("Bearer ".length()).trim();
            Optional<Client> client = clientRepository.findByApiKey(apikey);

            if (client.isEmpty()) {
                unauthorized(response, "Invalid Api key");
                return;
            }

            Client found = client.get();

            // Rate-limit per client, using their plan's limit (LAZY plan fetch happens here).
            int limit = found.getPlan().getRateLimitPerMinute();
            if (!rateLimiterService.isAllowed(found.getClientId(), limit)) {
                tooManyRequests(response);
                return;
            }

            CurrentClient.set(found);                    // attach client to this request
            filterChain.doFilter(request, response);      // continue to the controller
        } finally {
            CurrentClient.clear();                         // always clear (thread is reused)
        }
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);   // 401
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
    }

    private void tooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);                                    // Too Many Requests
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests\"}");
    }
}