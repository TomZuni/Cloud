package cl.education.enrollment.config;

import java.util.ArrayList;
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
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${app.security.allowed-audiences:}") String allowedAudiences,
            @Value("${app.security.allowed-policies:}") String allowedPolicies
    ) {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalStateException("AZURE_AD_ISSUER_URI debe configurarse para validar JWT en Spring.");
        }

        NimbusJwtDecoder decoder = hasText(jwkSetUri)
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                : NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));

        List<String> audiences = parseValues(allowedAudiences);
        if (!audiences.isEmpty()) {
            validators.add(new AudienceValidator(audiences));
        }

        List<String> policies = parseValues(allowedPolicies);
        if (!policies.isEmpty()) {
            validators.add(new AzureB2CPolicyValidator(policies));
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> parseValues(String value) {
        if (!hasText(value)) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
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

    private record AzureB2CPolicyValidator(List<String> allowedPolicies) implements OAuth2TokenValidator<Jwt> {

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            String policy = jwt.getClaimAsString("tfp");
            if (policy == null || policy.isBlank()) {
                policy = jwt.getClaimAsString("acr");
            }

            if (policy != null && allowedPolicies.contains(policy)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "El token JWT no pertenece a un flujo de usuario Azure B2C permitido.",
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
