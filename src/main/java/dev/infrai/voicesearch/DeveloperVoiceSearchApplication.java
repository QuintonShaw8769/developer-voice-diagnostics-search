package dev.infrai.voicesearch;

import dev.infrai.voicesearch.infrai.InfraiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InfraiProperties.class)
public class DeveloperVoiceSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeveloperVoiceSearchApplication.class, args);
    }
}
