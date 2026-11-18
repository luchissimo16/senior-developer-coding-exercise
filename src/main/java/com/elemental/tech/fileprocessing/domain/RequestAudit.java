package com.elemental.tech.fileprocessing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "request_audit")
public class RequestAudit {

    @Id
    private UUID requestId;

    @Column(nullable = false)
    private String requestUri;

    @Column(nullable = false)
    private Instant requestTimestamp;

    private int responseCode;

    private String requestIp;

    private String countryCode;

    private String provider;

    private long timeLapsedMs;

    public RequestAudit(
            UUID requestId,
            String requestUri,
            Instant requestTimestamp,
            int responseCode,
            String requestIp,
            String countryCode,
            String provider,
            long timeLapsedMs) {

        this.requestId = requestId;
        this.requestUri = requestUri;
        this.requestTimestamp = requestTimestamp;
        this.responseCode = responseCode;
        this.requestIp = requestIp;
        this.countryCode = countryCode;
        this.provider = provider;
        this.timeLapsedMs = timeLapsedMs;
    }
}
