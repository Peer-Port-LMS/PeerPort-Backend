package peerport.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import peerport.backend.service.AuthService;
import peerport.backend.model.RoleModel.Role;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled=true)
public class SecurityConfig {

    @Autowired
    private AuthService authService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable()) // Enable Cross-Site Request Forgery protection later
            .authorizeHttpRequests(auth -> auth
                // Allow preflight OPTIONS requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Authenticated endpoints
                .requestMatchers("/users/**").access((authentication, context) ->
                    new AuthorizationDecision(authService.hasAnyRole(Role.STUDENT, Role.INSTRUCTOR, Role.ADMIN))
                )
                .requestMatchers("/courses/**").access((authentication, context) ->
                    new AuthorizationDecision(authService.hasAnyRole(Role.STUDENT, Role.INSTRUCTOR, Role.ADMIN))
                )
                .requestMatchers("/assignments/**").access((authentication, context) ->
                    new AuthorizationDecision(authService.hasAnyRole(Role.STUDENT, Role.ADMIN, Role.INSTRUCTOR))
                )
                .requestMatchers("/content/**").access((authentication, context) ->
                    new AuthorizationDecision(authService.hasAnyRole(Role.STUDENT, Role.ADMIN, Role.INSTRUCTOR))
                )
                .requestMatchers("/enrollments/**").access((authentication, context) ->
                    new AuthorizationDecision(authService.hasAnyRole(Role.ADMIN, Role.INSTRUCTOR))
                )
                .requestMatchers("/grades/**").access((authentication, context) ->
                    new AuthorizationDecision(authService.hasAnyRole(Role.ADMIN, Role.INSTRUCTOR))
                )

                // Public endpoints
                .anyRequest().permitAll()
            )

            // Enable OAuth2 login
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("http://localhost:5173/login?redirect=true", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(authService)
                )
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration conf = new CorsConfiguration();
        conf.setAllowedOrigins(List.of("http://localhost:5173"));
        conf.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        conf.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        conf.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", conf);
        return source;
    }
}
