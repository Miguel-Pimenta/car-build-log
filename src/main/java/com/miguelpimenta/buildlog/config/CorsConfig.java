package com.miguelpimenta.buildlog.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets a browser frontend served from a different origin (e.g. the Next.js dev
 * server at http://localhost:3000) call this API. Browsers block cross-origin
 * requests unless the server opts in via CORS.
 *
 * Allowed origins are configurable through the {@code app.cors.allowed-origins}
 * property (comma-separated). In production, set it to the deployed frontend URL,
 * e.g. {@code APP_CORS_ALLOWED_ORIGINS=https://your-frontend.example.com}.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
