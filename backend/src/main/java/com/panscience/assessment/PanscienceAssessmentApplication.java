package com.panscience.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PanscienceAssessmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PanscienceAssessmentApplication.class, args);
    }
}

