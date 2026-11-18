package com.elemental.tech.fileprocessing.api.dto;

public record IpApiResponse(
        String status,
        String message,
        String countryCode,
        String isp,
        String org,
        String asname
) {
}
