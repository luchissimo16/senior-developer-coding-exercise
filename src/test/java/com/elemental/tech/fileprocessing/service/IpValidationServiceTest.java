package com.elemental.tech.fileprocessing.service;

import com.elemental.tech.fileprocessing.api.dto.IpApiResponse;
import com.elemental.tech.fileprocessing.config.IpValidationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IpValidationServiceTest {

    private IpValidationService service;

    @BeforeEach
    void setUp() {
        IpValidationProperties properties = new IpValidationProperties();
        properties.setBlockedCountries(List.of("CN", "ES", "US"));
        properties.setBlockedProviders(List.of("AWS", "GCP", "Azure"));

        service = new IpValidationService(
                RestClient.builder(),
                "http://localhost",
                properties
        );
    }

    @Test
    void shouldAllowNonBlockedCountryAndIsp() {
        assertNull(service.blockingReason(success("GB", "Example Residential ISP")));
    }

    @Test
    void shouldBlockChina() {
        assertEquals(
                "Blocked country: CN",
                service.blockingReason(success("CN", "China Telecom"))
        );
    }

    @Test
    void shouldBlockSpain() {
        assertEquals(
                "Blocked country: ES",
                service.blockingReason(success("ES", "Telefonica"))
        );
    }

    @Test
    void shouldBlockUsa() {
        assertEquals(
                "Blocked country: US",
                service.blockingReason(success("US", "Comcast"))
        );
    }

    @Test
    void shouldBlockAws() {
        assertEquals(
                "Blocked provider: AWS",
                service.blockingReason(success("GB", "AWS"))
        );
    }

    @Test
    void shouldBlockGcp() {
        assertEquals(
                "Blocked provider: GCP",
                service.blockingReason(success("GB", "GCP"))
        );
    }

    @Test
    void shouldBlockAzure() {
        assertEquals(
                "Blocked provider: Azure",
                service.blockingReason(success("GB", "Azure"))
        );
    }

    @Test
    void shouldNotTreatGenericGoogleAsGcp() {
        assertNull(service.blockingReason(success("GB", "Google LLC")));
    }

    @Test
    void shouldThrowWhenLookupUnsuccessful() {
        IpApiResponse failed = new IpApiResponse(
                "fail",
                "invalid query",
                null,
                null,
                null,
                null
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.blockingReason(failed)
        );
    }

    private static IpApiResponse success(String countryCode, String isp) {
        return new IpApiResponse(
                "success",
                null,
                countryCode,
                isp,
                isp,
                "EXAMPLE"
        );
    }
}
