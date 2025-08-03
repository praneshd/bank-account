package tech.challenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class for Spring Security.
 * Defines security-related beans and configurations for the application.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true) // Enables method-level security annotations
public class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests.
     * Disables CSRF protection, configures authorization rules, and enables HTTP Basic authentication.
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disables CSRF protection
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/balance").authenticated() // Requires authentication for specific endpoints
                        .anyRequest().permitAll() // Allows all other requests
                )
                .httpBasic(httpBasic -> {}); // Enables HTTP Basic authentication

        return http.build();
    }

    /**
     * Configures an in-memory user details service with a single user.
     * The user has a username, an encoded password, and a role.
     *
     * @return the configured UserDetailsService
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("test") // Sets the username
                .password(passwordEncoder().encode("p@ssword12")) // Encodes the password securely
                .roles("USER") // Assigns the role "USER"
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    /**
     * Configures a password encoder using BCrypt.
     * BCrypt is a strong hashing algorithm for secure password storage.
     *
     * @return the configured PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}