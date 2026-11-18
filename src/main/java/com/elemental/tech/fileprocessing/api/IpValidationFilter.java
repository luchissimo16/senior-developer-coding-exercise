package com.elemental.tech.fileprocessing.api;

import com.elemental.tech.fileprocessing.api.dto.IpApiResponse;
import com.elemental.tech.fileprocessing.domain.RequestAudit;
import com.elemental.tech.fileprocessing.service.IpValidationService;
import com.elemental.tech.fileprocessing.service.RequestAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class IpValidationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(IpValidationFilter.class);

    private final IpValidationService validationService;
    private final RequestAuditService auditService;
    private final ObjectMapper objectMapper;

    public IpValidationFilter(
            IpValidationService validationService,
            RequestAuditService auditService,
            ObjectMapper objectMapper) {

        this.validationService = validationService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        return !(
                "POST".equalsIgnoreCase(request.getMethod()) &&
                        "/api/files/process".equals(path)
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        UUID requestId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long start = System.nanoTime();
        String ip = request.getRemoteAddr();
        IpApiResponse data = null;

        try {
            if (ip == null || ip.isBlank()) {
                writeError(response, 400, "Client IP is missing");
                return;
            }

            ValidationResult result = validateIp(ip, response);
            data = result.data();
            if (result.allowed()) {
                chain.doFilter(request, response);
            }
        } catch (Exception ex) {
            if (!response.isCommitted()) {
                response.setStatus(500);
            }
            throw ex;
        } finally {
            saveAudit(requestId, request, response, ip, data, timestamp, start);
        }
    }

    private ValidationResult validateIp(
            String ip,
            HttpServletResponse response) throws IOException {

        try {
            IpApiResponse data = validationService.lookup(ip);
            String reason = validationService.blockingReason(data);

            if (reason != null) {
                writeError(response, 403, reason);
                return ValidationResult.blocked(data);
            }

            return ValidationResult.allowed(data);
        } catch (Exception ex) {
            log.warn("IP validation failed for {}", ip, ex);
            writeError(response, 503, "IP validation service unavailable");
            return ValidationResult.unavailable();
        }
    }

    private void saveAudit(
            UUID requestId,
            HttpServletRequest request,
            HttpServletResponse response,
            String ip,
            IpApiResponse data,
            Instant timestamp,
            long startNanos) {

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos
        );

        RequestAudit audit = new RequestAudit(
                requestId,
                request.getRequestURI(),
                timestamp,
                response.getStatus(),
                ip,
                data == null ? null : data.countryCode(),
                data == null ? null : data.isp(),
                elapsedMs
        );

        try {
            auditService.save(audit);
        } catch (Exception auditEx) {
            log.error("Failed to persist request audit {}", requestId, auditEx);
        }
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(
                response.getWriter(),
                Map.of("error", message)
        );
    }

    private record ValidationResult(IpApiResponse data, boolean allowed) {

        static ValidationResult allowed(IpApiResponse data) {
            return new ValidationResult(data, true);
        }

        static ValidationResult blocked(IpApiResponse data) {
            return new ValidationResult(data, false);
        }

        static ValidationResult unavailable() {
            return new ValidationResult(null, false);
        }
    }
}
