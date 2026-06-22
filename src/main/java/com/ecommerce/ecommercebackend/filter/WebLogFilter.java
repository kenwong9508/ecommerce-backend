package com.ecommerce.ecommercebackend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class WebLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebLogFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Wrap the request to allow reading the request body multiple times
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 1024 * 1024);

        // 2. Record the start time of the request
        long startTime = System.currentTimeMillis();

        try {
            // 3. Let the request proceed to the Controller
            filterChain.doFilter(requestWrapper, response);
        } finally {
            // 4. Calculate the execution time in milliseconds
            long duration = System.currentTimeMillis() - startTime;

            // 5. Extract basic request metadata
            String method = requestWrapper.getMethod();
            String uri = requestWrapper.getRequestURI();
            int status = response.getStatus();
            String clientIp = requestWrapper.getRemoteAddr();

            // 6. Extract the request JSON body payload
            String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            // Remove whitespaces and line breaks for better single-line logging
            requestBody = requestBody.replaceAll("\\s", "");

            // 7. Print the structured log to the console
            LOGGER.info(
                    "API Log | Method: {} | URI: {} | Body: {} | Status: {} | Time: {}ms | IP: {} ",
                    method,
                    uri,
                    requestBody.isEmpty() ? "{}" : requestBody,
                    status,
                    duration,
                    clientIp);
        }
    }

    // Optional: Exclude specific paths from being logged (e.g., static resources, Swagger docs)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/favicon.ico") || path.startsWith("/swagger-ui");
    }
}
