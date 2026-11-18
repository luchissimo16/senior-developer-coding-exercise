package com.elemental.tech.fileprocessing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ip-validation")
public class IpValidationProperties {

    private List<String> blockedCountries = new ArrayList<>();
    private List<String> blockedProviders = new ArrayList<>();
}
