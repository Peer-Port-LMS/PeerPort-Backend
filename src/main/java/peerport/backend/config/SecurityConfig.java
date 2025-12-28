package peerport.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable()) // Enable Cross-Site Request Forgery protection later
            .authorizeHttpRequests(auth -> auth
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
}
