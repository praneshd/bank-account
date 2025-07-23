package tech.challenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.function.Supplier;

@Configuration
public class ApplicationBeanConfig {

    @Bean
    public Supplier<Double> randomDoubleSupplier() {
        return () -> new Random().nextDouble(); // [0.0, 1.0)
    }
}
