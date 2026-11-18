package com.elemental.tech.fileprocessing.service;

import com.elemental.tech.fileprocessing.api.dto.IpApiResponse;
import com.elemental.tech.fileprocessing.config.IpValidationProperties;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IpValidationService {

    private final RestClient restClient;
    private final Set<String> blockedCountries;
    private final Map<String, Pattern> blockedProviders;

    public IpValidationService(
            RestClient.Builder builder,
            @Value("${ip-api.base-url}") String baseUrl,
            IpValidationProperties properties) {

        this.restClient = builder
                .baseUrl(baseUrl)
                .build();

        this.blockedCountries = properties.getBlockedCountries().stream()
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        Map<String, Pattern> providers = new LinkedHashMap<>();
        for (String provider : properties.getBlockedProviders()) {
            providers.put(
                    provider,
                    Pattern.compile(
                            "\\b" + Pattern.quote(provider) + "\\b",
                            Pattern.CASE_INSENSITIVE
                    )
            );
        }
        this.blockedProviders = Map.copyOf(providers);
    }

    @Retry(name = "ipApi")
    public IpApiResponse lookup(String ip) {

        IpApiResponse body = restClient.get()
                .uri("/json/{ip}?fields=status,message,countryCode,isp,org,asname",
                        ip)
                .retrieve()
                .body(IpApiResponse.class);

        if (body == null) {
            throw new IllegalStateException(
                    "Empty response from IP validation service"
            );
        }

        return body;
    }

    public String blockingReason(IpApiResponse data) {

        if (data == null ||
                !"success".equals(data.status()) ||
                data.countryCode() == null ||
                data.countryCode().isBlank()) {

            throw new IllegalStateException(
                    "Unable to validate IP address"
            );
        }

        String countryCode = data.countryCode().toUpperCase(Locale.ROOT);
        if (blockedCountries.contains(countryCode)) {
            return "Blocked country: " + data.countryCode();
        }

        String providerText = String.join(" ",
                safe(data.isp()),
                safe(data.org()),
                safe(data.asname())
        );

        for (Map.Entry<String, Pattern> entry : blockedProviders.entrySet()) {
            if (entry.getValue().matcher(providerText).find()) {
                return "Blocked provider: " + entry.getKey();
            }
        }

        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
