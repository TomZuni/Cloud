package cl.education.enrollment.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
    public SecurityFilterChain jwtSecurityFilterChain(
            HttpSecurity http,
            @Value("${app.security.download-authorities:SCOPE_guides.download,ROLE_GUIDES_DOWNLOAD}") String downloadAuthorities,
            @Value("${app.security.manage-authorities:SCOPE_guides.manage,ROLE_GUIDES_MANAGE}") String manageAuthorities
    ) throws Exception {
        String[] downloadOrManage = mergeAuthorities(downloadAuthorities, manageAuthorities);
        String[] manageOnly = toAuthorityArray(manageAuthorities);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dispatch-guides/queue-log/**").hasAnyAuthority(downloadOrManage)
                        .requestMatchers("/api/rabbit-admin/**").hasAnyAuthority(manageOnly)
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
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
            @Value("${app.security.issuer-uri:}") String issuerUri,
            @Value("${app.security.jwk-set-uri:}") String jwkSetUri,
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

    private String[] toAuthorityArray(String value) {
        return parseValues(value).toArray(String[]::new);
    }

    private String[] mergeAuthorities(String first, String second) {
        List<String> authorities = new ArrayList<>();
        authorities.addAll(parseValues(first));
        authorities.addAll(parseValues(second));
        return authorities.stream()
                .distinct()
                .toArray(String[]::new);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));

            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.stream()
                        .filter(this::hasText)
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }

            return authorities;
        });

        return converter;
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
