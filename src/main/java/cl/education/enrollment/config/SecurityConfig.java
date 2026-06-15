package cl.education.enrollment.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(
            name = "app.security.jwt-validation-enabled",
            havingValue = "true"
    )
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.security.jwt-validation-enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public SecurityFilterChain gatewaySecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.security.jwt-validation-enabled",
            havingValue = "true"
    )
    public NimbusJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
            @Value("${app.security.allowed-audiences:}") String allowedAudiences
    ) {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalStateException("AZURE_AD_ISSUER_URI debe configurarse para validar JWT en Spring.");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        List<String> audiences = parseAudiences(allowedAudiences);

        if (audiences.isEmpty()) {
            decoder.setJwtValidator(issuerValidator);
            return decoder;
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator,
                new AudienceValidator(audiences)
        ));
        return decoder;
    }

    private List<String> parseAudiences(String allowedAudiences) {
        if (allowedAudiences == null || allowedAudiences.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedAudiences.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private record AudienceValidator(List<String> allowedAudiences) implements OAuth2TokenValidator<Jwt> {

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            boolean accepted = jwt.getAudience()
                    .stream()
                    .anyMatch(allowedAudiences::contains);

            if (accepted) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "El token JWT no contiene una audiencia permitida para esta API.",
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
