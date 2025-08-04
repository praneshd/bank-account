package tech.challenge;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Random;
import java.util.function.Supplier;

@TestConfiguration
public class TestConfig {

    @Bean
    public Supplier<Double> randomSupplier() {
        return () -> new Random().nextDouble(); // [0.0, 1.0)
    }
}
