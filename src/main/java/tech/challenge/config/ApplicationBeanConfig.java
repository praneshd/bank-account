package tech.challenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Configuration class for defining application-level beans.
 * Provides bean definitions for dependency injection.
 */
@Configuration
public class ApplicationBeanConfig {

    /**
     * Defines a bean that supplies random double values.
     * The supplier generates random double values in the range [0.0, 1.0).
     *
     * @return a Supplier of random double values
     */
    @Bean
    public Supplier<Double> randomDoubleSupplier() {
        return () -> new Random().nextDouble(); // [0.0, 1.0)
    }
}