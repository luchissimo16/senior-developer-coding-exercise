package com.elemental.tech;

import com.elemental.tech.fileprocessing.config.IpValidationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IpValidationProperties.class)
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
