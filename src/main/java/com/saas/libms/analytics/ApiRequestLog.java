package com.saas.libms.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "api_request_logs",
        indexes = {
                @Index(name = "idx_api_log_endpoint",       columnList = "endpoint"),
                @Index(name = "idx_api_log_status_code",    columnList = "status_code"),
                @Index(name = "idx_api_log_institution_id", columnList = "institution_id"),
                @Index(name = "idx_api_log_created_at",     columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * HTTP method + path, normalized.
     * Example: "GET /api/v1/books", "POST /api/v1/auth/login"
     * Path variables are replaced with {id} to avoid cardinality explosion:
     * "GET /api/v1/books/BOOK-K3MP9R" → "GET /api/v1/books/{id}"
     */
    @Column(name = "endpoint", nullable = false, length = 120)
    private String endpoint;

    /**
     * HTTP response status code: 200, 201, 400, 401, 403, 404, 429, 500 etc.
     */
    @Column(name = "status_code", nullable = false)
    private int statusCode;

    /**
     * Total time from request received to response sent, in milliseconds.
     * Used to identify slow endpoints.
     */
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    /**
     * Institution ID of the authenticated user, if present.
     * Null for unauthenticated requests (login, register, forgot-password).
     * Used to filter traffic by institution.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    /**
     * Client IP address. Same extraction logic as RateLimitFilter.
     * Useful for abuse detection alongside rate limit data.
     */
    @Column(name = "client_ip", length = 60)
    private String clientIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
