package tech.challenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class ApplicationBeanConfig {

    @Bean
    public Random random() {
        return new Random(); // real random in production
    }
}
