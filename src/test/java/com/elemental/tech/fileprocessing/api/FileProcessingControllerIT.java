package com.elemental.tech.fileprocessing.api;

import com.elemental.tech.fileprocessing.domain.RequestAudit;
import com.elemental.tech.fileprocessing.persistence.RequestAuditRepository;
import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class FileProcessingControllerIT {

    static final WireMockServer wireMock =
            new WireMockServer(wireMockConfig().dynamicPort());

    static {
        wireMock.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "ip-api.base-url",
                wireMock::baseUrl
        );
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    RequestAuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        auditRepository.deleteAll();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void shouldAllowIpFromUnitedKingdom() throws Exception {

        stubIp(
                "203.0.113.10",
                "GB",
                "Example Residential ISP"
        );

        Instant before = Instant.now();

        sendFile("203.0.113.10")
                .andExpect(status().isOk());

        Instant after = Instant.now();

        List<RequestAudit> audits = auditRepository.findAll();
        assertEquals(1, audits.size());

        RequestAudit audit = audits.get(0);
        assertEquals(200, audit.getResponseCode());
        assertEquals("GB", audit.getCountryCode());
        assertEquals("Example Residential ISP", audit.getProvider());
        assertEquals("/api/files/process", audit.getRequestUri());
        assertEquals("203.0.113.10", audit.getRequestIp());
        assertNotNull(audit.getRequestId());
        assertNotNull(audit.getRequestTimestamp());
        assertFalse(audit.getRequestTimestamp().isBefore(before.minusSeconds(1)));
        assertFalse(audit.getRequestTimestamp().isAfter(after.plusSeconds(1)));
        assertTrue(audit.getTimeLapsedMs() >= 0);

        wireMock.verify(
                1,
                getRequestedFor(urlPathEqualTo("/json/203.0.113.10"))
        );
    }

    @Test
    void shouldBlockChina() throws Exception {

        stubIp("203.0.113.20", "CN", "China Telecom");

        sendFile("203.0.113.20")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Blocked country: CN"));

        assertBlockedAudit("CN", "China Telecom");
    }

    @Test
    void shouldBlockSpain() throws Exception {

        stubIp("203.0.113.21", "ES", "Telefonica");

        sendFile("203.0.113.21")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Blocked country: ES"));

        assertBlockedAudit("ES", "Telefonica");
    }

    @Test
    void shouldBlockUsa() throws Exception {

        stubIp("203.0.113.22", "US", "Comcast Cable");

        sendFile("203.0.113.22")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Blocked country: US"));

        assertBlockedAudit("US", "Comcast Cable");
    }

    @Test
    void shouldBlockAWS() throws Exception {

        stubIp("203.0.113.30", "GB", "AWS");

        sendFile("203.0.113.30")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Blocked provider: AWS"));

        assertBlockedAudit("GB", "AWS");
    }

    @Test
    void shouldBlockGcp() throws Exception {

        stubIp("203.0.113.31", "GB", "GCP");

        sendFile("203.0.113.31")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Blocked provider: GCP"));

        assertBlockedAudit("GB", "GCP");
    }

    @Test
    void shouldBlockAzure() throws Exception {

        stubIp("203.0.113.32", "GB", "Azure");

        sendFile("203.0.113.32")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Blocked provider: Azure"));

        assertBlockedAudit("GB", "Azure");
    }

    private void assertBlockedAudit(String countryCode, String provider) {
        List<RequestAudit> audits = auditRepository.findAll();
        assertEquals(1, audits.size());

        RequestAudit audit = audits.get(0);
        assertEquals(403, audit.getResponseCode());
        assertEquals(countryCode, audit.getCountryCode());
        assertEquals(provider, audit.getProvider());
        assertEquals("/api/files/process", audit.getRequestUri());
        assertNotNull(audit.getRequestId());
        assertNotNull(audit.getRequestTimestamp());
        assertTrue(audit.getTimeLapsedMs() >= 0);
    }

    private void stubIp(
            String ip,
            String countryCode,
            String isp) {

        wireMock.stubFor(
                get(urlPathEqualTo("/json/" + ip))
                        .willReturn(
                                okJson("""
                                        {
                                          "status": "success",
                                          "countryCode": "%s",
                                          "isp": "%s",
                                          "org": "%s",
                                          "asname": "EXAMPLE"
                                        }
                                        """.formatted(
                                        countryCode, isp, isp
                                ))
                        )
        );
    }

    private ResultActions sendFile(String ip) throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "example.txt",
                "text/plain",
                "Hello from integration test".getBytes()
        );

        return mvc.perform(
                multipart("/api/files/process")
                        .file(file)
                        .with(request -> {
                            request.setRemoteAddr(ip);
                            return request;
                        })
        );
    }
}
