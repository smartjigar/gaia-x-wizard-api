package eu.gaiax.wizard.config.security;

import eu.gaiax.wizard.config.security.model.SecurityConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static eu.gaiax.wizard.api.utils.RoleConstant.PARTICIPANT_ROLE;
import static eu.gaiax.wizard.utils.WizardRestConstant.*;

@Slf4j
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@Configuration
@RequiredArgsConstructor
public class AuthenticationConfig {
    private final SecurityConfigProperties configProperties;

    @Bean
    @ConditionalOnProperty(value = "wizard.security.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authRequest -> authRequest
                        .requestMatchers("/", "/docs/api-docs/**", "/ui/swagger-ui/**", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/ingress/**").permitAll()
                        .requestMatchers("/did/**").permitAll()
                        .requestMatchers("/certificate/**").permitAll()
                        .requestMatchers("/.well-known/**").permitAll()
                        .requestMatchers(API_PUBLIC_URL_PREFIX + "/**").permitAll()
                        .requestMatchers(PARTICIPANT_JSON).permitAll()
                        .requestMatchers(ONBOARD_PARTICIPANT, PARTICIPANT_ROOT + "/**").hasRole(PARTICIPANT_ROLE)
                        .requestMatchers(SERVICE_OFFER).hasRole(PARTICIPANT_ROLE)
                        .requestMatchers(TENANT + "/**").hasRole(PARTICIPANT_ROLE)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(new CustomAuthenticationConverter(configProperties.clientId()))))
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "wizard.security.enabled", havingValue = "false")
    public WebSecurityCustomizer securityCustomizer() {
        log.warn("AuthenticationConfig(securityCustomizer) : Disable security -> This is not recommended to use in production environments.");
        return web -> web.ignoring().requestMatchers(new AntPathRequestMatcher("**"));
    }

}


